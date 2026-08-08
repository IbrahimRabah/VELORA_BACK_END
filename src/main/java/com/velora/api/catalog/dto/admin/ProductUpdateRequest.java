package com.velora.api.catalog.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "Update a product")
public record ProductUpdateRequest(

        @NotNull Long categoryId,

        Long brandId,

        @Schema(description = """
                Changing a published slug breaks existing links and search rankings.
                The server records the old value in url_redirect automatically.
                """)
        String slug,

        @Valid List<TranslationRequest> translations,

        boolean featured,

        boolean newArrival,

        @Valid List<ProductCreateRequest.SpecificationRequest> specifications
) {
}
