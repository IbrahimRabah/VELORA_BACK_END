package com.velora.api.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Product or variant image")
public record ImageResponse(
        Long id,
        String url,
        String thumbUrl,
        String alt,
        boolean main
) {
}
