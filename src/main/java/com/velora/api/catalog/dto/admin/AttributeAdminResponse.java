package com.velora.api.catalog.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * An attribute as seen by staff, with both locale names and every allowed value.
 *
 * <p>{@code id} and each value's {@code id} are meant to be used as-is: they map
 * directly onto {@code VariantMatrixRequest.AttributeSelection.attributeId} and
 * {@code valueIds} for {@code POST /admin/products/{productId}/variants/preview}.
 */
@Schema(description = "Attribute as seen by staff, with all values and translations")
public record AttributeAdminResponse(
        Long id,
        String code,
        String dataType,
        boolean variantDefining,
        boolean filterable,
        int displayOrder,
        String nameAr,
        String nameEn,
        List<ValueResponse> values
) {

    @Schema(description = "One allowed value, as seen by staff")
    public record ValueResponse(
            Long id,
            String code,
            @Schema(example = "#C9A227") String hexColor,
            int displayOrder,
            String nameAr,
            String nameEn
    ) {
    }
}
