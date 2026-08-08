package com.velora.api.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Add a variant to the cart")
public record AddToCartRequest(

        @Schema(description = "The VARIANT id, not the product id", example = "1")
        @NotNull(message = "Variant is required")
        Long variantId,

        @Schema(example = "1")
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 99, message = "Maximum 99 per line")
        Integer quantity
) {
}
