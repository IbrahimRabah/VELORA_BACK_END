package com.velora.api.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Void an invoice. The number stays consumed.")
public record CancelInvoiceRequest(

        @Schema(example = "خطأ في بيانات العميل")
        @NotBlank(message = "A reason is required to cancel an invoice")
        @Size(max = 255) String reason
) {
}
