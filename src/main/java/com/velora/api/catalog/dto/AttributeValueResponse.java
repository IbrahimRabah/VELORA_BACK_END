package com.velora.api.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "One allowed value of an attribute")
public record AttributeValueResponse(
        Long id,
        String code,
        String name,
        @Schema(example = "#C9A227", description = "For colour swatches")
        String hexColor,
        @Schema(description = "Number of matching products, for filter facets")
        Long productCount
) {
    public static AttributeValueResponse of(Long id, String code, String name, String hexColor) {
        return new AttributeValueResponse(id, code, name, hexColor, null);
    }
}
