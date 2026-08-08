package com.velora.api.shipping.web;

import com.velora.api.catalog.web.LocaleResolver;
import com.velora.api.identity.security.UserPrincipal;
import com.velora.api.shipping.dto.ShippingQuoteRequest;
import com.velora.api.shipping.dto.ShippingQuoteResponse;
import com.velora.api.shipping.service.ShippingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Shipping", description = "Delivery cost and estimate")
@RestController
@RequestMapping("/api/v1/shipping")
public class ShippingController {

    private static final String GUEST_HEADER = "X-Guest-Token";

    private final ShippingService shippingService;

    public ShippingController(ShippingService shippingService) {
        this.shippingService = shippingService;
    }

    @Operation(summary = "Quote shipping for the current cart",
            description = """
                    Used by the cart-page estimator and again at checkout. Cheap enough to
                    call every time the customer changes governorate.

                    Cash on delivery is assumed, since it is the only payment method in V1.
                    """,
            security = {})
    @ApiResponse(responseCode = "409", description = "We do not deliver to that governorate")
    @PostMapping("/quote")
    public ShippingQuoteResponse quote(@AuthenticationPrincipal UserPrincipal principal,
                                       @RequestHeader(value = GUEST_HEADER, required = false)
                                       String guestToken,
                                       @Valid @RequestBody ShippingQuoteRequest body,
                                       HttpServletRequest request) {

        return shippingService.quote(
                body.governorateId(),
                principal == null ? null : principal.id(),
                guestToken,
                body.cartId(),
                true,
                LocaleResolver.resolve(request));
    }
}
