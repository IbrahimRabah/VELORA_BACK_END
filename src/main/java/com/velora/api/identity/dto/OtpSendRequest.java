package com.velora.api.identity.dto;

import com.velora.api.identity.domain.OtpPurpose;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request a one-time code")
public record OtpSendRequest(

        @Schema(example = "01012345678", description = "Mobile number or email address")
        @NotBlank(message = "Destination is required")
        String destination,

        @Schema(example = "REGISTER")
        @NotNull(message = "Purpose is required")
        OtpPurpose purpose
) {
}
