package com.velora.api.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(description = "An issued invoice")
public record InvoiceResponse(
        Long id,
        @Schema(example = "VLR-INV-2026-000001") String invoiceNumber,
        String status,
        Long orderId,
        String orderNumber,
        String buyerName,
        String buyerPhone,
        BigDecimal grandTotal,
        BigDecimal taxTotal,
        BigDecimal netTotal,
        String currency,
        @Schema(description = "Null when the PDF has not been stored yet")
        String pdfUrl,
        OffsetDateTime issuedAt,
        String cancelReason
) {
}
