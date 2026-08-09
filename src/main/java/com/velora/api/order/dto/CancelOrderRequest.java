package com.velora.api.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Cancel an order before it ships")
public record CancelOrderRequest(

        @Schema(example = "غيرت رأيي")
        @Size(max = 255) String reason
) {
}
