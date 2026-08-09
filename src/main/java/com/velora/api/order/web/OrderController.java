package com.velora.api.order.web;

import com.velora.api.catalog.web.LocaleResolver;
import com.velora.api.common.dto.PageResponse;
import com.velora.api.identity.security.UserPrincipal;
import com.velora.api.order.dto.CancelOrderRequest;
import com.velora.api.order.dto.OrderResponse;
import com.velora.api.order.dto.OrderSummaryResponse;
import com.velora.api.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "My Orders", description = "The customer's own order history")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/me/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "My orders, newest first")
    @GetMapping
    public PageResponse<OrderSummaryResponse> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable,
            HttpServletRequest request) {

        return orderService.listForCustomer(principal.id(), pageable,
                LocaleResolver.resolve(request));
    }

    @Operation(summary = "One order in full",
            description = """
                    Includes the status timeline. Note the TWO status fields: render them
                    as separate badges. `DELIVERED` with `paymentStatus: PENDING` is a
                    normal COD parcel awaiting courier remittance.
                    """)
    @GetMapping("/{orderNumber}")
    public OrderResponse get(@AuthenticationPrincipal UserPrincipal principal,
                             @PathVariable String orderNumber,
                             HttpServletRequest request) {

        return orderService.getForCustomer(principal.id(), orderNumber,
                LocaleResolver.resolve(request));
    }

    @Operation(summary = "Cancel an order",
            description = "Allowed until the parcel is dispatched. Stock is returned.")
    @PostMapping("/{orderNumber}/cancel")
    public OrderResponse cancel(@AuthenticationPrincipal UserPrincipal principal,
                                @PathVariable String orderNumber,
                                @Valid @RequestBody CancelOrderRequest body,
                                HttpServletRequest request) {

        return orderService.cancelByCustomer(principal.id(), orderNumber, body.reason(),
                LocaleResolver.resolve(request));
    }
}
