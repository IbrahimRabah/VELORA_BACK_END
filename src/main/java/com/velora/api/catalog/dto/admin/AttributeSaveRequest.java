package com.velora.api.catalog.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.List;

@Schema(description = "Create or update an attribute")
public record AttributeSaveRequest(

        @Schema(example = "COLOR")
        @NotBlank(message = "Code is required")
        @Pattern(regexp = "[A-Z0-9_]+", message = "Code must be UPPER_SNAKE_CASE")
        String code,

        @Schema(example = "LIST", allowableValues = {"LIST", "TEXT", "NUMBER", "BOOLEAN"})
        String dataType,

        @Schema(description = """
                TRUE generates SKUs — colour, size. It multiplies the number of variants.
                FALSE is specification only — movement, water resistance, fragrance notes.
                This is the single most consequential flag on an attribute.
                """)
        boolean variantDefining,

        boolean filterable,

        Integer displayOrder,

        @NotEmpty(message = "At least one translation is required")
        @Valid
        List<NameTranslation> translations,

        @Valid List<ValueRequest> values
) {

    @Schema(description = "Attribute name in one locale")
    public record NameTranslation(
            @NotBlank String locale,
            @NotBlank String name
    ) {
    }

    @Schema(description = "One allowed value")
    public record ValueRequest(
            Long id,
            @NotBlank @Pattern(regexp = "[A-Z0-9_]+") String code,
            @Schema(example = "#C9A227") String hexColor,
            Integer displayOrder,
            @NotEmpty @Valid List<NameTranslation> translations
    ) {
    }
}
