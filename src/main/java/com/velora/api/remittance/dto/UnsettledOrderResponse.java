package com.velora.api.remittance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * A delivered order whose cash has not arrived.
 *
 * <p>{@code daysWaiting} is the field that matters. A balance two days old is normal;
 * the same balance three weeks old is a conversation with the courier.
 */
@Schema(description = "Money the courier still owes")
public record UnsettledOrderResponse(
        Long orderId,
        String orderNumber,
        String customerName,
        String governorate,
        BigDecimal amount,
        OffsetDateTime deliveredAt,
        @Schema(description = "Days since delivery — age is what makes it worth chasing")
        int daysWaiting
) {
}
