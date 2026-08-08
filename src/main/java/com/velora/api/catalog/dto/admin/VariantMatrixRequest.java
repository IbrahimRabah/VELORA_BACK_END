package com.velora.api.catalog.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Ask the server for every combination of the selected attribute values.
 *
 * <p>Preview first, then delete the combinations you are not stocking, then save.
 * Four attributes with five values each is 625 rows — almost none of which will
 * ever exist. Never persist a generated matrix unreviewed.
 */
@Schema(description = "Generate the variant matrix")
public record VariantMatrixRequest(

        @NotEmpty(message = "Select at least one attribute")
        @Valid
        List<AttributeSelection> selections
) {

    @Schema(description = "One attribute and the values to include")
    public record AttributeSelection(

            @NotNull Long attributeId,

            @NotEmpty(message = "Select at least one value")
            List<Long> valueIds
    ) {
    }
}
