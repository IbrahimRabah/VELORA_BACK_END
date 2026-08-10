package com.velora.api.export.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * Which orders to export.
 *
 * <p>Every field is optional, but exporting the whole table is almost never what
 * anyone wants — a 5,000-row sheet is not something a person reads. The service
 * caps the result and says so.
 */
@Schema(description = "Export filters")
public record OrderExportFilter(

        @Schema(example = "2026-08-01", description = "Inclusive")
        LocalDate dateFrom,

        @Schema(example = "2026-08-31", description = "Inclusive — the whole day counts")
        LocalDate dateTo,

        @Schema(example = "CONFIRMED",
                description = "Fulfilment status. Use CONFIRMED for a picking list.")
        String fulfillmentStatus,

        @Schema(example = "PENDING", description = "Payment status")
        String paymentStatus,

        @Schema(description = "Limit to one governorate")
        Long governorateId,

        @Schema(description = "Exclude cancelled orders. Default true for accounting.")
        Boolean excludeCancelled
) {

    public boolean shouldExcludeCancelled() {
        return excludeCancelled == null || excludeCancelled;
    }
}
