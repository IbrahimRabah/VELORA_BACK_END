package com.velora.api.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Exchange a refresh token for a new access token")
public record TokenRefreshRequest(

        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
}
