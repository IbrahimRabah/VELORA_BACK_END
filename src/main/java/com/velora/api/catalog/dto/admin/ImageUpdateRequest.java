package com.velora.api.catalog.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Update image metadata")
public record ImageUpdateRequest(

        @Schema(description = "Attach to one variant so the gallery swaps with colour. "
                + "Null means shared across all variants.")
        Long variantId,

        @Schema(description = "Accessibility and SEO requirement")
        @Size(max = 255) String altTextAr,

        @Size(max = 255) String altTextEn,

        Boolean main,

        Integer displayOrder
) {
}
