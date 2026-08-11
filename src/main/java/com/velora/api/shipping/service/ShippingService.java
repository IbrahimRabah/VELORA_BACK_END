package com.velora.api.shipping.service;

import com.velora.api.cart.domain.Cart;
import com.velora.api.cart.domain.CartItem;
import com.velora.api.cart.domain.CartStatus;
import com.velora.api.cart.repository.CartRepository;
import com.velora.api.cart.security.GuestTokenService;
import com.velora.api.common.exception.BusinessException;
import com.velora.api.common.exception.ErrorCode;
import com.velora.api.common.util.MoneyUtils;
import com.velora.api.shipping.domain.Governorate;
import com.velora.api.shipping.domain.ShippingRate;
import com.velora.api.shipping.dto.GovernorateResponse;
import com.velora.api.shipping.dto.ShippingQuoteResponse;
import com.velora.api.shipping.repository.GovernorateRepository;
import com.velora.api.shipping.repository.ShippingRateRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shipping quotes and the governorate list.
 *
 * <p>Everything is driven by the governorate. Postal codes are not usable in this
 * market, so the governorate is both what the customer picks and what determines
 * the price.
 */
@Service
@Transactional(readOnly = true)
public class ShippingService {

    private static final Logger log = LoggerFactory.getLogger(ShippingService.class);

    private final GovernorateRepository governorateRepository;
    private final ShippingRateRepository rateRepository;
    private final CartRepository cartRepository;
    private final ShippingCalculator calculator;
    private final GuestTokenService guestTokenService;

    public ShippingService(GovernorateRepository governorateRepository,
                           ShippingRateRepository rateRepository,
                           CartRepository cartRepository,
                           ShippingCalculator calculator,
                           GuestTokenService guestTokenService) {
        this.governorateRepository = governorateRepository;
        this.rateRepository = rateRepository;
        this.cartRepository = cartRepository;
        this.calculator = calculator;
        this.guestTokenService = guestTokenService;
    }

    /**
     * All governorates with their rate, for the address form and the shipping
     * estimator.
     *
     * <p>A governorate with no configured rate is returned with {@code served =
     * false} rather than hidden — the customer should see their governorate and be
     * told we do not deliver there, not silently fail to find it in the list.
     */
    public List<GovernorateResponse> listGovernorates(String locale) {
        List<Governorate> governorates =
                governorateRepository.findByActiveTrueOrderByDisplayOrderAsc();

        List<GovernorateResponse> result = new ArrayList<>();
        for (Governorate governorate : governorates) {
            Optional<ShippingRate> rate =
                    rateRepository.findForGovernorate(governorate.getId());

            result.add(new GovernorateResponse(
                    governorate.getId(),
                    governorate.getCode(),
                    governorate.nameFor(locale),
                    rate.map(r -> r.getZone().nameFor(locale)).orElse(null),
                    rate.map(ShippingRate::getBaseCost).orElse(null),
                    rate.map(r -> (int) r.getDeliveryDaysMin()).orElse(null),
                    rate.map(r -> (int) r.getDeliveryDaysMax()).orElse(null),
                    rate.isPresent()));
        }
        return result;
    }

    /**
     * What delivery will cost for this cart to this governorate.
     *
     * <p>Called from the cart page estimator and again at checkout. Cheap enough to
     * call on every governorate change.
     */
    public ShippingQuoteResponse quote(Long governorateId, Long userId, String guestToken,
                                       Long explicitCartId, boolean codApplies,
                                       String locale) {

        Governorate governorate = governorateRepository.findById(governorateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Governorate not found"));

        ShippingRate rate = rateRepository.findForGovernorate(governorateId)
                .orElseThrow(() -> {
                    log.warn("No shipping rate configured for governorate {} ({})",
                            governorateId, governorate.getCode());
                    return new BusinessException(ErrorCode.GOVERNORATE_NOT_SERVED,
                            "We do not deliver to %s yet".formatted(governorate.nameFor(locale)));
                });

        Cart cart = resolveCart(userId, guestToken, explicitCartId);
        BigDecimal subtotal = subtotalOf(cart);
        int weight = weightOf(cart);

        var calculation = calculator.calculate(rate, subtotal, weight, codApplies);

        return new ShippingQuoteResponse(
                governorate.getId(),
                governorate.nameFor(locale),
                rate.getZone().nameFor(locale),
                calculation.shippingCost(),
                calculation.baseCost(),
                calculation.codFee(),
                calculation.freeShippingApplied(),
                calculation.freeShippingThreshold(),
                calculation.amountToFreeShipping(),
                rate.getDeliveryDaysMin(),
                rate.getDeliveryDaysMax(),
                subtotal,
                weight,
                MoneyUtils.round(subtotal.add(calculation.totalDeliveryCharge())));
    }

    /**
     * The rate for an order being created. Throws rather than returning empty,
     * because an order cannot be priced without it.
     */
    public ShippingRate requireRateFor(Long governorateId) {
        return rateRepository.findForGovernorate(governorateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHIPPING_RATE_NOT_CONFIGURED,
                        "No shipping rate is configured for this governorate"));
    }

    public ShippingCalculator getCalculator() {
        return calculator;
    }

    // ------------------------------------------------------------------ internal

    private Cart resolveCart(Long userId, String guestToken, Long explicitCartId) {
        if (explicitCartId != null) {
            return cartRepository.findByIdAndStatus(explicitCartId, CartStatus.ACTIVE)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CART_EMPTY));
        }
        if (userId != null) {
            return cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CART_EMPTY));
        }
        if (guestToken != null && !guestToken.isBlank()) {
            // Resolves a cart straight from the guest token, the same as
            // CartService.findOrCreate() — same verification for the same reason: a
            // quote leaks cart subtotal and weight to anyone holding the token.
            guestTokenService.verify(guestToken);
            return cartRepository.findByGuestTokenAndStatus(guestToken, CartStatus.ACTIVE)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CART_EMPTY));
        }
        throw new BusinessException(ErrorCode.CART_EMPTY,
                "Sign in, or send an X-Guest-Token header");
    }

    /** Priced from the CURRENT variant price, exactly like the cart does. */
    private BigDecimal subtotalOf(Cart cart) {
        BigDecimal subtotal = MoneyUtils.ZERO;
        for (CartItem item : cart.getItems()) {
            subtotal = subtotal.add(
                    MoneyUtils.lineTotal(item.getVariant().getPrice(), item.getQuantity()));
        }
        return MoneyUtils.round(subtotal);
    }

    /**
     * Total weight, from the variants. This is why {@code weightGrams} sits on the
     * variant and not the product: a 42 mm steel watch and a 38 mm one do not weigh
     * the same, and the courier bills the difference.
     */
    private int weightOf(Cart cart) {
        int total = 0;
        for (CartItem item : cart.getItems()) {
            total += item.getVariant().getWeightGrams() * item.getQuantity();
        }
        return total;
    }
}
