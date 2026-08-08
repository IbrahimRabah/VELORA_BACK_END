package com.velora.api.catalog.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Generated combinations, for review before saving")
public record VariantMatrixPreviewResponse(

        int totalCombinations,

        @Schema(description = "How many already exist and would be skipped")
        int existingCount,

        List<Combination> combinations,

        @Schema(description = "e.g. capped at 200 combinations")
        List<String> warnings
) {

    @Schema(description = "One generated combination")
    public record Combination(
            @Schema(example = "VLR-CLASSIC-GLD-42") String suggestedSku,
            @Schema(example = "ذهبي / 42 مم") String summary,
            List<Long> attributeValueIds,
            List<String> valueNames,
            @Schema(description = "True when a variant with this combination already exists")
            boolean alreadyExists
    ) {
    }
}
