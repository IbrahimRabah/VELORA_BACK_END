package com.velora.api.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * One field for both identifiers. The server decides whether it looks like a phone
 * or an email — the client should not have to.
 */
@Schema(description = "Sign in with either a mobile number or an email")
public record LoginRequest(

        @Schema(example = "01012345678", description = "Mobile number or email address",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Phone or email is required")
        String identifier,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Password is required")
        String password,

        @Schema(example = "Chrome on Windows", description = "Optional, shown in active sessions")
        String deviceInfo
) {
}
