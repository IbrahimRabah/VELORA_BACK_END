package com.velora.api.shipping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "An Egyptian governorate, with its shipping terms")
public record GovernorateResponse(
        Long id,
        String code,
        String name,
        @Schema(description = "Zone name, for display only") String zoneName,
        @Schema(example = "70.00") BigDecimal shippingCost,
        Integer deliveryDaysMin,
        Integer deliveryDaysMax,
        @Schema(description = "False when we do not deliver there yet") boolean served
) {
}
