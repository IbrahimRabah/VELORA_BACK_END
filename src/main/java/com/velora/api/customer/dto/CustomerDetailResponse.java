package com.velora.api.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "One customer in full")
public record CustomerDetailResponse(
        Long id,
        String firstName,
        String lastName,
        String phone,
        String email,
        boolean phoneVerified,
        boolean emailVerified,
        String status,
        String locale,
        OffsetDateTime registeredAt,
        OffsetDateTime lastLoginAt,
        List<String> roles,

        @Schema(description = "Purchase history summary")
        Purchases purchases,

        @Schema(description = "Saved delivery addresses")
        List<AddressLine> addresses,

        @Schema(description = "Recent orders, newest first")
        List<OrderLine> recentOrders
) {

    @Schema(description = "What this customer is worth")
    public record Purchases(
            int totalOrders,
            int deliveredOrders,
            @Schema(description = "Refusals and failed deliveries — a warning sign for COD")
            int failedOrders,
            int cancelledOrders,
            BigDecimal totalSpent,
            BigDecimal averageOrderValue,
            OffsetDateTime firstOrderAt,
            OffsetDateTime lastOrderAt
    ) {
    }

    public record AddressLine(
            Long id,
            String label,
            String recipientName,
            String phone,
            String governorate,
            String formatted,
            boolean isDefault
    ) {
    }

    public record OrderLine(
            Long id,
            String orderNumber,
            String fulfillmentStatus,
            String paymentStatus,
            BigDecimal grandTotal,
            int itemCount,
            OffsetDateTime placedAt
    ) {
    }
}
