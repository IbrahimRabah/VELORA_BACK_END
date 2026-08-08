package com.velora.api.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Receive goods from a supplier")
public record StockReceiveRequest(

        @NotNull @Positive(message = "Quantity must be positive")
        Integer quantity,

        @Schema(example = "PO-2026-0042")
        @Size(max = 60) String reference,

        @Size(max = 500) String note
) {
}
