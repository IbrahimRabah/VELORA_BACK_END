package com.velora.api.order.service;

import com.velora.api.cart.domain.Cart;
import com.velora.api.cart.domain.CartItem;
import com.velora.api.cart.service.CartService;
import com.velora.api.catalog.domain.Product;
import com.velora.api.catalog.domain.ProductImage;
import com.velora.api.catalog.domain.ProductTranslation;
import com.velora.api.catalog.domain.ProductVariant;
import com.velora.api.common.exception.BusinessException;
import com.velora.api.common.exception.ErrorCode;
import com.velora.api.common.util.MoneyUtils;
import com.velora.api.common.util.PhoneNormalizer;
import com.velora.api.customer.domain.CustomerAddress;
import com.velora.api.customer.service.AddressService;
import com.velora.api.identity.domain.AppUser;
import com.velora.api.identity.repository.AppUserRepository;
import com.velora.api.inventory.service.ReservationService;
import com.velora.api.order.domain.CustomerOrder;
import com.velora.api.order.domain.FulfillmentStatus;
import com.velora.api.order.domain.OrderItem;
import com.velora.api.order.domain.OrderStatusHistory;
import com.velora.api.order.domain.PaymentMethod;
import com.velora.api.order.domain.PaymentStatus;
import com.velora.api.order.domain.StatusKind;
import com.velora.api.order.dto.PlaceOrderRequest;
import com.velora.api.order.repository.OrderRepository;
import com.velora.api.order.repository.OrderStatusHistoryRepository;
import com.velora.api.shipping.domain.Governorate;
import com.velora.api.shipping.domain.ShippingRate;
import com.velora.api.shipping.repository.GovernorateRepository;
import com.velora.api.shipping.service.ShippingCalculator;
import com.velora.api.shipping.service.ShippingService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns a cart into an order.
 *
 * <p>A Facade on purpose: checkout touches the cart, inventory, shipping, pricing
 * and the order itself. One orchestrator with ONE transaction boundary is far safer
 * than five services calling each other, because a failure at any step must undo
 * everything — most importantly the stock reservations.
 *
 * <p>Three rules govern this class:
 * <ol>
 *   <li><b>Reserve before writing.</b> If stock is gone, nothing else should happen.</li>
 *   <li><b>Snapshot everything.</b> Names, prices, tax rates and the address are
 *       copied, never referenced.</li>
 *   <li><b>Allocate the discount to the lines.</b> Without it, a partial return has
 *       no correct refund amount and it cannot be worked out later.</li>
 * </ol>
 */
@Service
public class CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);

    private final CartService cartService;
    private final ReservationService reservationService;
    private final ShippingService shippingService;
    private final ShippingCalculator shippingCalculator;
    private final AddressService addressService;
    private final GovernorateRepository governorateRepository;
    private final AppUserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final OrderNumberGenerator orderNumberGenerator;

    public CheckoutService(CartService cartService,
                           ReservationService reservationService,
                           ShippingService shippingService,
                           ShippingCalculator shippingCalculator,
                           AddressService addressService,
                           GovernorateRepository governorateRepository,
                           AppUserRepository userRepository,
                           OrderRepository orderRepository,
                           OrderStatusHistoryRepository historyRepository,
                           OrderNumberGenerator orderNumberGenerator) {
        this.cartService = cartService;
        this.reservationService = reservationService;
        this.shippingService = shippingService;
        this.shippingCalculator = shippingCalculator;
        this.addressService = addressService;
        this.governorateRepository = governorateRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.historyRepository = historyRepository;
        this.orderNumberGenerator = orderNumberGenerator;
    }

    /**
     * Places an order. All of it, or none of it.
     *
     * <p>The single transaction is what makes stock safe: if anything after the
     * reservation throws, the reservation rolls back with it and the units return to
     * available. No compensation logic, no orphaned holds.
     */
    @Transactional
    public CustomerOrder placeOrder(Long userId, String guestToken,
                                    PlaceOrderRequest request, String locale) {

        // 1. Cart — refuses if empty or if anything blocks checkout.
        Cart cart = cartService.loadForCheckout(userId, guestToken);

        // 2. Address, resolved to a snapshot before anything is written.
        AddressSnapshot address = resolveAddress(userId, request);
        Governorate governorate = governorateRepository.findById(address.governorateId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Governorate not found"));

        // 3. Shipping. Throws before any stock is touched if we do not deliver there.
        ShippingRate rate = shippingService.requireRateFor(governorate.getId());

        // 4. Reserve stock. FIRST write of the transaction, so a shortage costs
        //    nothing but a rolled-back read.
        Map<ProductVariant, Integer> quantities = new LinkedHashMap<>();
        for (CartItem item : cart.getItems()) {
            quantities.merge(item.getVariant(), item.getQuantity(), Integer::sum);
        }
        reservationService.reserveAll(quantities, cart.getId());

        // 5. Money.
        Totals totals = calculateTotals(cart, rate);

        // 6. The order, with everything copied.
        CustomerOrder order = new CustomerOrder();
        order.setOrderNumber(orderNumberGenerator.generate());
        order.setCustomer(userId == null ? null : userRepository.findById(userId).orElse(null));
        order.setPaymentMethod(resolvePaymentMethod(request.paymentMethod()));
        order.setFulfillmentStatus(FulfillmentStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setLocale(locale);
        order.setCustomerNote(request.customerNote());

        applyContactAndAddress(order, address, governorate);

        order.setShippingZoneName(rate.getZone().nameFor(locale));
        order.setDeliveryDaysMin(rate.getDeliveryDaysMin());
        order.setDeliveryDaysMax(rate.getDeliveryDaysMax());

        order.setSubtotalGross(totals.subtotal());
        order.setDiscountTotal(totals.discount());
        order.setShippingCost(totals.shipping());
        order.setCodFee(totals.codFee());
        order.setGrandTotal(totals.grandTotal());
        order.setTaxTotal(totals.tax());
        order.setNetTotal(totals.net());

        buildItems(order, cart, totals, locale);

        CustomerOrder saved = orderRepository.save(order);

        historyRepository.save(OrderStatusHistory.of(saved, StatusKind.FULFILLMENT,
                null, FulfillmentStatus.PENDING.name(), "Order placed", userId));

        // 7. Hand the holds to the order, and retire the cart.
        reservationService.attachToOrder(cart.getId(), saved.getId());
        cartService.markConverted(cart.getId());

        log.info("Order {} placed: {} item(s), {} {}, {} to {}",
                saved.getOrderNumber(), saved.getItems().size(),
                saved.getGrandTotal(), saved.getCurrency(),
                saved.getPaymentMethod(), saved.getShipGovernorateName());

        // TODO(notification module): publish OrderPlacedEvent so the SMS is sent
        // AFTER_COMMIT — never inside this transaction, or a rolled-back order
        // still texts the customer.

        return saved;
    }

    // ------------------------------------------------------------------- money

    /**
     * All figures are TAX-INCLUSIVE, so tax is extracted rather than added.
     *
     * <p>Computed per line and then summed. Doing it on the order total instead
     * makes the invoice disagree with its own lines by a piastre or two — the kind
     * of thing an accountant notices immediately.
     */
    private Totals calculateTotals(Cart cart, ShippingRate rate) {
        BigDecimal subtotal = MoneyUtils.ZERO;
        List<BigDecimal> lineTotals = new ArrayList<>();

        for (CartItem item : cart.getItems()) {
            BigDecimal lineTotal = MoneyUtils.lineTotal(
                    item.getVariant().getPrice(), item.getQuantity());
            lineTotals.add(lineTotal);
            subtotal = subtotal.add(lineTotal);
        }
        subtotal = MoneyUtils.round(subtotal);

        // TODO(promotion module): resolve the cart's coupon into a real amount.
        // The allocation below already works — only this number is missing.
        BigDecimal discount = MoneyUtils.ZERO;

        // The share of the discount each line carries. Stored on the line, because
        // after the order exists there is no way to derive it again.
        List<BigDecimal> allocations = MoneyUtils.allocate(discount, lineTotals);

        int weight = 0;
        for (CartItem item : cart.getItems()) {
            weight += item.getVariant().getWeightGrams() * item.getQuantity();
        }

        ShippingCalculator.Calculation shipping = shippingCalculator.calculate(
                rate, subtotal, weight, true);

        // Tax per line, on the discounted amount, then summed.
        BigDecimal tax = MoneyUtils.ZERO;
        for (int i = 0; i < cart.getItems().size(); i++) {
            CartItem item = cart.getItems().get(i);
            BigDecimal taxableGross = lineTotals.get(i).subtract(allocations.get(i));
            tax = tax.add(MoneyUtils.taxFromGross(taxableGross, item.getVariant().getTaxRate()));
        }

        BigDecimal grandTotal = MoneyUtils.round(subtotal
                .subtract(discount)
                .add(shipping.shippingCost())
                .add(shipping.codFee()));

        // Shipping is treated as untaxed here. Confirm with an accountant — if it is
        // taxable, the tax comes out of the shipping charge, it is not added to it.
        BigDecimal net = MoneyUtils.round(grandTotal.subtract(tax));

        return new Totals(subtotal, discount, shipping.shippingCost(), shipping.codFee(),
                grandTotal, MoneyUtils.round(tax), net, lineTotals, allocations);
    }

    /** Copies every line. Nothing here may be resolved live afterwards. */
    private void buildItems(CustomerOrder order, Cart cart, Totals totals, String locale) {
        List<CartItem> cartItems = cart.getItems();

        for (int i = 0; i < cartItems.size(); i++) {
            CartItem cartItem = cartItems.get(i);
            ProductVariant variant = cartItem.getVariant();
            Product product = variant.getProduct();

            BigDecimal lineTotal = totals.lineTotals().get(i);
            BigDecimal allocated = totals.allocations().get(i);
            BigDecimal taxableGross = lineTotal.subtract(allocated);

            OrderItem item = new OrderItem();
            item.setVariant(variant);
            item.setProduct(product);

            // ---- the snapshot ----
            item.setProductNameAr(nameFor(product, "ar"));
            item.setProductNameEn(nameFor(product, "en"));
            item.setSku(variant.getSku());
            item.setVariantSummary(summaryFor(variant, locale));
            ProductImage image = product.mainImage();
            item.setImageUrl(image == null ? null : image.getUrl());
            item.setUnitPriceGross(variant.getPrice());
            item.setQuantity(cartItem.getQuantity());
            item.setLineDiscount(MoneyUtils.ZERO);
            item.setAllocatedCartDiscount(allocated);
            item.setTaxRate(variant.getTaxRate());
            item.setLineTotalGross(lineTotal);
            item.setLineTaxAmount(
                    MoneyUtils.taxFromGross(taxableGross, variant.getTaxRate()));

            order.addItem(item);
        }
    }

    // ----------------------------------------------------------------- address

    private AddressSnapshot resolveAddress(Long userId, PlaceOrderRequest request) {
        if (request.addressId() != null) {
            if (userId == null) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED,
                        "Sign in to use a saved address");
            }
            // Ownership is enforced inside requireOwned.
            CustomerAddress saved = addressService.requireOwned(userId, request.addressId());

            return new AddressSnapshot(
                    saved.getRecipientName(), saved.getPhoneE164(), saved.getAltPhoneE164(),
                    null, saved.getGovernorate().getId(), saved.getArea(),
                    saved.getStreetAddress(), saved.getBuilding(), saved.getFloor(),
                    saved.getApartment(), saved.getLandmark());
        }

        PlaceOrderRequest.AddressInput input = request.address();
        if (input == null) {
            throw new BusinessException(ErrorCode.INVALID_ADDRESS,
                    "Provide a saved addressId or a full address");
        }

        String phone = PhoneNormalizer.toE164(input.phone());
        if (phone == null) {
            throw new BusinessException(ErrorCode.INVALID_PHONE_FORMAT);
        }
        String altPhone = input.altPhone() == null || input.altPhone().isBlank()
                ? null : PhoneNormalizer.toE164(input.altPhone());

        return new AddressSnapshot(
                input.recipientName().trim(), phone, altPhone, input.email(),
                input.governorateId(), input.area(), input.streetAddress().trim(),
                input.building(), input.floor(), input.apartment(), input.landmark());
    }

    private void applyContactAndAddress(CustomerOrder order, AddressSnapshot address,
                                        Governorate governorate) {
        order.setContactName(address.recipientName());
        order.setContactPhone(address.phone());
        order.setContactAltPhone(address.altPhone());
        order.setContactEmail(address.email());

        order.setShipGovernorate(governorate);
        order.setShipGovernorateName(governorate.getNameAr());
        // City is free text in V1. Falls back to the governorate so the NOT NULL
        // column always has something meaningful for the courier label.
        order.setShipCityName(address.area() != null && !address.area().isBlank()
                ? address.area() : governorate.getNameAr());
        order.setShipArea(address.area());
        order.setShipStreetAddress(address.streetAddress());
        order.setShipBuilding(address.building());
        order.setShipFloor(address.floor());
        order.setShipApartment(address.apartment());
        order.setShipLandmark(address.landmark());
    }

    // ------------------------------------------------------------------ helpers

    private PaymentMethod resolvePaymentMethod(String raw) {
        if (raw == null || raw.isBlank()) {
            return PaymentMethod.COD;
        }
        PaymentMethod method;
        try {
            method = PaymentMethod.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.PAYMENT_METHOD_UNAVAILABLE,
                    "Unknown payment method: " + raw);
        }
        if (method != PaymentMethod.COD) {
            throw new BusinessException(ErrorCode.PAYMENT_METHOD_UNAVAILABLE,
                    "Only cash on delivery is available at the moment");
        }
        return method;
    }

    private String nameFor(Product product, String locale) {
        ProductTranslation translation = product.getTranslations().get(locale);
        if (translation != null) {
            return translation.getName();
        }
        // The snapshot columns are NOT NULL, so fall back rather than fail an order
        // over a missing translation.
        return product.nameFor("ar");
    }

    private String summaryFor(ProductVariant variant, String locale) {
        if (variant.getAttributeValues().isEmpty()) {
            return null;
        }
        return variant.getAttributeValues().stream()
                .map(vav -> vav.getAttributeValue().nameFor(locale))
                .reduce((a, b) -> a + " / " + b)
                .orElse(null);
    }

    /** The address, flattened, before anything is written. */
    private record AddressSnapshot(
            String recipientName, String phone, String altPhone, String email,
            Long governorateId, String area, String streetAddress,
            String building, String floor, String apartment, String landmark) {
    }

    /** Every money figure for one order, computed once. */
    private record Totals(
            BigDecimal subtotal, BigDecimal discount, BigDecimal shipping, BigDecimal codFee,
            BigDecimal grandTotal, BigDecimal tax, BigDecimal net,
            List<BigDecimal> lineTotals, List<BigDecimal> allocations) {
    }
}
