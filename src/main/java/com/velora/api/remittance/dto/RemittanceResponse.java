package com.velora.api.remittance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "A recorded settlement")
public record RemittanceResponse(
        Long id,
        @Schema(example = "REM-2026-0001") String reference,
        String courierName,
        String courierReference,
        LocalDate settlementDate,
        @Schema(example = "SETTLED", allowableValues = {"SETTLED", "SHORT", "CANCELLED"})
        String status,
        @Schema(description = "Sum of the orders — what should have arrived")
        BigDecimal expectedAmount,
        BigDecimal receivedAmount,
        @Schema(description = "received − expected. Negative is a shortfall.")
        BigDecimal difference,
        int orderCount,
        String note,
        List<SettledOrder> orders,
        OffsetDateTime createdAt
) {

    @Schema(description = "One order in the batch, with the amount as reconciled")
    public record SettledOrder(Long orderId, String orderNumber, BigDecimal amount) {
    }
}
