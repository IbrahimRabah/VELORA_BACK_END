package com.velora.api.catalog.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * One locale's text for a translatable entity.
 *
 * <p>{@code searchText} is NOT accepted from the client — the server derives it by
 * running the name and description through {@code ArabicNormalizer}. Letting a
 * client supply it would guarantee it eventually diverges from the query-time
 * normalization, and search would silently stop matching.
 */
@Schema(description = "Translated content for one locale")
public record TranslationRequest(

        @Schema(example = "ar", allowableValues = {"ar", "en"})
        @NotBlank
        @Pattern(regexp = "ar|en", message = "Locale must be 'ar' or 'en'")
        String locale,

        @Schema(example = "ساعة كلاسيك ذهبية")
        @NotBlank(message = "Name is required")
        @Size(max = 255)
        String name,

        @Size(max = 500)
        String shortDescription,

        String description,

        @Size(max = 255)
        String metaTitle,

        @Size(max = 500)
        String metaDescription
) {
}
