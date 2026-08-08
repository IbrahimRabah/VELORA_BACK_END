package com.velora.api.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Change the quantity of a cart line")
public record UpdateCartItemRequest(

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Use DELETE to remove the item")
        @Max(value = 99, message = "Maximum 99 per line")
        Integer quantity
) {
}
