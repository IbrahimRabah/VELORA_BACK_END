package com.velora.api.identity.dto;

import com.velora.api.identity.domain.OtpPurpose;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Verify a one-time code")
public record OtpVerifyRequest(

        @NotBlank(message = "Destination is required")
        String destination,

        @Schema(example = "482913")
        @NotBlank(message = "Code is required")
        @Pattern(regexp = "\\d{6}", message = "The code is 6 digits")
        String code,

        @NotNull(message = "Purpose is required")
        OtpPurpose purpose
) {
}
