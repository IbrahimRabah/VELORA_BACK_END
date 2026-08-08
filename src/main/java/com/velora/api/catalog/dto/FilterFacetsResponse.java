package com.velora.api.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/**
 * Everything the filter sidebar needs, in one call.
 *
 * <p>Only values that actually occur on products in this category are returned —
 * offering a colour that yields zero results is a dead end for the customer.
 */
@Schema(description = "Available filters for a category")
public record FilterFacetsResponse(
        List<BrandResponse> brands,
        List<VariantOptionResponse> attributes,
        @Schema(description = "Cheapest product in this category")
        BigDecimal minPrice,
        @Schema(description = "Most expensive product in this category")
        BigDecimal maxPrice
) {
}
