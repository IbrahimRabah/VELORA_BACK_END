package com.velora.api.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/**
 * A sellable unit. This {@code id} is what goes into the cart — never the product id.
 *
 * <p>Note there is no cost price here, and there never should be.
 */
@Schema(description = "One sellable variant")
public record VariantResponse(
        Long id,
        String sku,
        @Schema(example = "ذهبي / 42 مم")
        String summary,
        @Schema(description = "Tax-inclusive final price")
        BigDecimal price,
        BigDecimal compareAtPrice,
        Integer discountPercent,
        @Schema(description = "The attribute value ids that define this variant")
        List<Long> attributeValueIds,
        @Schema(description = "on hand minus reserved")
        int availableQty,
        boolean inStock,
        List<ImageResponse> images
) {
}
