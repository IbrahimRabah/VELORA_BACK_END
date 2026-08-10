package com.velora.api.order.web;

import com.velora.api.catalog.web.LocaleResolver;
import com.velora.api.identity.security.UserPrincipal;
import com.velora.api.idempotency.service.IdempotencyService;
import com.velora.api.order.dto.OrderResponse;
import com.velora.api.order.dto.PlaceOrderRequest;
import com.velora.api.order.service.CheckoutService;
import com.velora.api.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Checkout", description = "Turn a cart into an order. Guest checkout supported.")
@RestController
@RequestMapping("/api/v1/orders")
public class CheckoutController {

    private static final String GUEST_HEADER = "X-Guest-Token";
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final CheckoutService checkoutService;
    private final OrderService orderService;
    private final IdempotencyService idempotencyService;

    public CheckoutController(CheckoutService checkoutService,
                              OrderService orderService,
                              IdempotencyService idempotencyService) {
        this.checkoutService = checkoutService;
        this.orderService = orderService;
        this.idempotencyService = idempotencyService;
    }

    @Operation(summary = "Place an order",
            description = """
                    Reserves stock, copies prices and the address onto the order, and
                    retires the cart — all in one transaction. If stock runs out at any
                    line, nothing is written and the holds are rolled back.

                    Supply either `addressId` (saved address, signed in) or `address`
                    inline (guest checkout). The address is COPIED, never referenced:
                    customers edit and delete addresses, and a delivered order must always
                    show where it actually went.

                    Cash on delivery only in V1. The order starts as
                    `PENDING` / `PENDING` — the second one stays pending until the courier
                    remits the cash, which is correct, not an error.

                    **Send an `Idempotency-Key`.** Generate one UUID per checkout attempt.
                    A repeat with the same key returns the SAME order instead of creating
                    another — which is what a double-tap on a slow connection would
                    otherwise do.
                    """,
            security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order created"),
            @ApiResponse(responseCode = "409",
                    description = "Stock ran out, or we do not deliver to that governorate"),
            @ApiResponse(responseCode = "400", description = "Cart empty or address incomplete")
    })
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = GUEST_HEADER, required = false) String guestToken,
            @Parameter(description = "Send a UUID so a double-tap on a slow connection "
                    + "cannot create two orders")
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody PlaceOrderRequest body,
            HttpServletRequest request) {

        String locale = LocaleResolver.resolve(request);
        Long userId = principal == null ? null : principal.id();

        /*
         * The key makes a repeated submit safe.
         *
         * A customer on a weak connection taps "place order", sees nothing happen, and
         * taps again. Without this the second tap creates a second order — real stock
         * reserved, an invoice eventually, and a refund conversation.
         *
         * Optional on purpose: a client that does not send the header still gets to
         * buy something. It is a safety net, not a gate.
         */
        return ResponseEntity.status(HttpStatus.CREATED).body(
                idempotencyService.execute(
                        idempotencyKey,
                        "POST /api/v1/orders",
                        body,
                        userId,
                        OrderResponse.class,
                        () -> {
                            var order = checkoutService.placeOrder(
                                    userId, guestToken, body, locale);

                            return userId == null
                                    ? orderService.getForAdmin(order.getId(), locale)
                                    : orderService.getForCustomer(
                                            userId, order.getOrderNumber(), locale);
                        }));
    }
}
