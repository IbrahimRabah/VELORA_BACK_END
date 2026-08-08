package com.velora.api.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Something changed since the customer added the item.
 *
 * <p>Warnings are the honest alternative to silently charging a different price or
 * quietly dropping a line. The front end shows them before checkout so the customer
 * confirms what they are actually buying.
 */
@Schema(description = "A change the customer should see before paying")
public record CartWarning(

        @Schema(example = "PRICE_CHANGED",
                allowableValues = {"PRICE_CHANGED", "QUANTITY_REDUCED",
                        "OUT_OF_STOCK", "PRODUCT_UNAVAILABLE"})
        String code,

        Long itemId,

        String sku,

        @Schema(description = "English. Translate on the client using the code.")
        String detail
) {

    public static CartWarning priceChanged(Long itemId, String sku, String detail) {
        return new CartWarning("PRICE_CHANGED", itemId, sku, detail);
    }

    public static CartWarning quantityReduced(Long itemId, String sku, String detail) {
        return new CartWarning("QUANTITY_REDUCED", itemId, sku, detail);
    }

    public static CartWarning outOfStock(Long itemId, String sku) {
        return new CartWarning("OUT_OF_STOCK", itemId, sku, "This item is now out of stock");
    }

    public static CartWarning unavailable(Long itemId, String sku) {
        return new CartWarning("PRODUCT_UNAVAILABLE", itemId, sku,
                "This item is no longer for sale");
    }
}
