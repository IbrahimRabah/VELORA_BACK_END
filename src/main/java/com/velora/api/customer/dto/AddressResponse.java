package com.velora.api.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A saved delivery address")
public record AddressResponse(
        Long id,
        String label,
        String recipientName,
        String phone,
        String altPhone,
        Long governorateId,
        String governorateName,
        String area,
        String streetAddress,
        String building,
        String floor,
        String apartment,
        String landmark,
        boolean isDefault,
        @Schema(description = "One line, for courier labels") String formatted
) {
}
