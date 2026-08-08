package com.velora.api.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Start a password reset")
public record ForgotPasswordRequest(

        @Schema(example = "01012345678", description = "Mobile number or email address")
        @NotBlank(message = "Phone or email is required")
        String identifier
) {
}
