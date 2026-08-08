package com.velora.api.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "A delivery address, shaped for Egypt")
public record AddressRequest(

        @Schema(example = "HOME", allowableValues = {"HOME", "WORK", "OTHER"})
        @Size(max = 30) String label,

        @Schema(example = "محمد أحمد",
                description = "May differ from the account holder — gifts are common")
        @NotBlank(message = "Recipient name is required")
        @Size(max = 150) String recipientName,

        @Schema(example = "01012345678", description = "Normalized to E.164 by the server")
        @NotBlank(message = "Phone number is required")
        String phone,

        @Schema(description = "A failed delivery is usually an unanswered phone")
        String altPhone,

        @NotNull(message = "Governorate is required")
        Long governorateId,

        @Schema(example = "المنيا الجديدة", description = "Free text")
        @Size(max = 150) String area,

        @Schema(example = "شارع الجمهورية")
        @NotBlank(message = "Street address is required")
        @Size(max = 255) String streetAddress,

        @Schema(example = "12") @Size(max = 50) String building,
        @Schema(example = "3") @Size(max = 20) String floor,
        @Schema(example = "5") @Size(max = 20) String apartment,

        @Schema(example = "بجوار مسجد النور",
                description = "Genuinely used by Egyptian couriers")
        @Size(max = 255) String landmark,

        @Schema(description = "Make this the default delivery address")
        Boolean makeDefault
) {
}
