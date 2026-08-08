package com.velora.api.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "Stock position for one variant")
public record InventoryAdminResponse(
        Long variantId,
        String sku,
        String productName,
        String variantSummary,
        int qtyOnHand,
        @Schema(description = "Committed to in-flight checkouts and unshipped orders")
        int qtyReserved,
        @Schema(description = "on hand minus reserved — the only number a customer sees")
        int qtyAvailable,
        int minStockLevel,
        boolean lowStock,
        boolean outOfStock,
        OffsetDateTime updatedAt
) {
}
