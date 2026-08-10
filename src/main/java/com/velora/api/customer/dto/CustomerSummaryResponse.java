package com.velora.api.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(description = "A customer, as a row in a list")
public record CustomerSummaryResponse(
        Long id,
        String name,
        String phone,
        String email,
        boolean phoneVerified,
        @Schema(description = "Orders that were not cancelled") int orderCount,
        @Schema(description = "Lifetime value") BigDecimal totalSpent,
        OffsetDateTime lastOrderAt,
        OffsetDateTime registeredAt,
        @Schema(description = "SUSPENDED accounts cannot sign in") String status
) {
}
