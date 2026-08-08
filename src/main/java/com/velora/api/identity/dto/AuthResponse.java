package com.velora.api.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Issued tokens and the signed-in user")
public record AuthResponse(

        @Schema(description = "Send as: Authorization: Bearer <token>")
        String accessToken,

        @Schema(description = "Used only against POST /api/v1/auth/refresh")
        String refreshToken,

        @Schema(example = "Bearer")
        String tokenType,

        @Schema(description = "Access token lifetime in seconds", example = "1800")
        long expiresIn,

        UserResponse user
) {
    public static AuthResponse of(String accessToken, String refreshToken,
                                  long expiresIn, UserResponse user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
}
