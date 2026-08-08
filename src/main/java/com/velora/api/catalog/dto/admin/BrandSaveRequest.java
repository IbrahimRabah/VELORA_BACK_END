package com.velora.api.catalog.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Create or update a brand")
public record BrandSaveRequest(

        String slug,

        @NotBlank(message = "Arabic name is required")
        @Size(max = 150) String nameAr,

        @NotBlank(message = "English name is required")
        @Size(max = 150) String nameEn,

        String logoUrl,

        Boolean active
) {
}
