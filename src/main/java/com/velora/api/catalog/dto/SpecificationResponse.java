package com.velora.api.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "One row of the specification table")
public record SpecificationResponse(
        String code,
        String name,
        String value
) {
}
