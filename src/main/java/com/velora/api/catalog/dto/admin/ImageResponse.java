package com.velora.api.catalog.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Uploaded product image")
public record ImageResponse(
        Long id,
        @Schema(description = "Storage key — this is what is stored in the database")
        String key,
        String url,
        String thumbUrl,
        String altTextAr,
        String altTextEn,
        boolean main,
        int displayOrder,
        Long variantId
) {
}
