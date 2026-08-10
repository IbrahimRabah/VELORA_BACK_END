package com.velora.api.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "The seller details, and anything still missing")
public record StoreProfileResponse(
        String legalName,
        String legalNameEn,
        String address,
        String phone,
        String email,
        String taxNumber,
        String commercialRegister,
        String website,
        String invoiceFooterNote,

        @Schema(description = "Fields that are empty and will not appear on invoices")
        List<String> missingFields
) {
}
