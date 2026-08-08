package com.velora.api.catalog.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Variant as seen by staff — includes cost price")
public record VariantAdminResponse(
        Long id,
        Long productId,
        String sku,
        String barcode,
        String summary,
        BigDecimal price,
        BigDecimal compareAtPrice,
        @Schema(description = "Owner-only") BigDecimal costPrice,
        BigDecimal taxRate,
        int weightGrams,
        String status,
        List<Long> attributeValueIds,
        int qtyOnHand,
        int qtyReserved,
        int qtyAvailable,
        int minStockLevel,
        boolean lowStock
) {
}
