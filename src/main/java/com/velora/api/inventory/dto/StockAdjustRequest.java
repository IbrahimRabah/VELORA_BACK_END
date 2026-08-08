package com.velora.api.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A manual correction to on-hand stock.
 *
 * <p>The reason is REQUIRED, not optional. An unexplained stock change is
 * indistinguishable from theft, and six months later nobody remembers.
 */
@Schema(description = "Manually adjust stock")
public record StockAdjustRequest(

        @Schema(description = "Signed. -2 for breakage, +5 for a found box.", example = "-2")
        @NotNull(message = "Quantity is required")
        Integer quantityDelta,

        @Schema(example = "Physical count correction after stocktake")
        @NotBlank(message = "A reason is required for every manual adjustment")
        @Size(max = 500)
        String reason,

        @Schema(example = "MANUAL_ADJUSTMENT",
                allowableValues = {"MANUAL_ADJUSTMENT", "DAMAGE_WRITEOFF", "PURCHASE_RECEIVED"})
        String movementType
) {
}
