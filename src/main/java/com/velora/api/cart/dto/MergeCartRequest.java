package com.velora.api.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Fold a guest cart into the signed-in account's cart")
public record MergeCartRequest(

        @Schema(description = "The X-Guest-Token used before signing in")
        @NotBlank(message = "Guest token is required")
        String guestToken
) {
}
