package com.velora.api.catalog.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "Create or update a category")
public record CategorySaveRequest(

        Long parentId,

        String slug,

        @NotEmpty(message = "At least one translation is required")
        @Valid
        List<TranslationRequest> translations,

        String imageUrl,

        String bannerUrl,

        Integer displayOrder,

        Boolean active
) {
}
