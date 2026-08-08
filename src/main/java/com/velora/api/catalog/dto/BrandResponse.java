package com.velora.api.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Brand")
public record BrandResponse(
        Long id,
        String slug,
        String name,
        String logoUrl
) {
}
