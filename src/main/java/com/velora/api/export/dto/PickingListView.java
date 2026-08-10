package com.velora.api.export.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * The view model the picking-list template renders.
 *
 * <p>Kept separate from the entities so the template never triggers a lazy load —
 * with {@code open-in-view: false} that would throw halfway through rendering.
 */
public record PickingListView(
        String title,
        String generatedAt,
        int orderCount,
        int totalItems,
        BigDecimal totalCodAmount,
        List<OrderBlock> orders
) {

    /** One order: its header, its lines, and what the courier must collect. */
    public record OrderBlock(
            String orderNumber,
            String placedAt,
            String customerName,
            String phone,
            String altPhone,
            String governorate,
            String address,
            String landmark,
            String note,
            String paymentMethod,
            BigDecimal codAmount,
            int totalQuantity,
            List<LineItem> lines
    ) {
    }

    /** One product to pick off the shelf. */
    public record LineItem(
            String sku,
            String name,
            String variant,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {
    }
}
