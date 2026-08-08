package com.velora.api.catalog.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * Save reviewed variants — used both by the matrix flow (batch) and when adding a
 * single new colour later.
 */
@Schema(description = "Create or update variants in one call")
public record VariantSaveRequest(

        @NotEmpty(message = "At least one variant is required")
        @Valid
        List<VariantItem> variants
) {

    @Schema(description = "One variant")
    public record VariantItem(

            @Schema(description = "Null to create, set to update an existing variant")
            Long id,

            @Schema(example = "VLR-CLASSIC-GLD-42")
            @Size(max = 60)
            String sku,

            @Size(max = 60)
            String barcode,

            @Schema(description = "TAX-INCLUSIVE final price in EGP", example = "2400.00")
            @NotNull(message = "Price is required")
            @DecimalMin(value = "0.0", message = "Price cannot be negative")
            BigDecimal price,

            @Schema(description = "The struck-through 'was' price")
            BigDecimal compareAtPrice,

            @Schema(description = "Owner-only. Never exposed publicly.")
            BigDecimal costPrice,

            @Schema(example = "0.1400")
            BigDecimal taxRate,

            @Schema(description = "Drives weight-based shipping")
            @PositiveOrZero
            Integer weightGrams,

            @Schema(description = "The combination defining this variant")
            List<Long> attributeValueIds,

            @Schema(description = "Opening stock. Writes a PURCHASE_RECEIVED movement.")
            @PositiveOrZero
            Integer initialStock,

            @PositiveOrZero
            Integer minStockLevel
    ) {
    }
}
