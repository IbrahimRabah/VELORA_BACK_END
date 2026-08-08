package com.velora.api.shipping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "Set the rate for a zone")
public record ShippingRateRequest(

        @NotNull(message = "Zone is required")
        Long zoneId,

        @Schema(example = "70.00")
        @NotNull(message = "Base cost is required")
        @DecimalMin(value = "0.0", message = "Cost cannot be negative")
        BigDecimal baseCost,

        @Schema(description = "Weight included in the base cost. Null ignores weight.")
        Integer maxWeightGrams,

        @Schema(description = "Charge per extra kilogram. Zero for a flat rate.")
        BigDecimal costPerExtraKg,

        @Schema(description = "Free above this order value. Null to never offer it.")
        BigDecimal freeShippingOver,

        @Schema(description = "Cash-on-delivery handling fee")
        BigDecimal codFee,

        @Min(0) Integer deliveryDaysMin,
        @Min(0) Integer deliveryDaysMax
) {
}
