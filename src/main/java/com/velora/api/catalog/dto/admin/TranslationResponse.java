package com.velora.api.catalog.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One locale's translated content, as seen by staff — the read-side counterpart of
 * {@link TranslationRequest}, same shape, so the admin form can load a product,
 * edit a field or two, and send the array straight back.
 *
 * <p>Sending back only what a narrower response exposed (e.g. just {@code name})
 * would silently blank out {@code shortDescription}, {@code description} and the
 * meta fields on save — {@code ProductAdminService.applyTranslations} overwrites
 * every field of a translation it is given, it does not merge.
 */
@Schema(description = "Translated content for one locale, as seen by staff")
public record TranslationResponse(
        String locale,
        String name,
        String shortDescription,
        String description,
        String metaTitle,
        String metaDescription
) {
}
