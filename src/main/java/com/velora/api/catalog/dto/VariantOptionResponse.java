package com.velora.api.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** One selector on the product page — "Colour" with its swatches. */
@Schema(description = "A variant-defining attribute and its available values")
public record VariantOptionResponse(
        Long attributeId,
        String code,
        String name,
        List<AttributeValueResponse> values
) {
}
