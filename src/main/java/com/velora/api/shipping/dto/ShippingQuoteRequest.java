package com.velora.api.shipping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Ask what shipping will cost")
public record ShippingQuoteRequest(

        @Schema(example = "19", description = "Where it is going")
        @NotNull(message = "Governorate is required")
        Long governorateId,

        @Schema(description = "Defaults to the caller's active cart")
        Long cartId
) {
}
