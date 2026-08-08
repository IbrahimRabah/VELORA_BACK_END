package com.velora.api.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * At least one of {@code email} or {@code phone} must be supplied — validated in
 * the service, because Bean Validation cannot express "one of these two".
 */
@Schema(description = "New account registration")
public record RegisterRequest(

        @Schema(example = "محمد", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "First name is required")
        @Size(max = 100)
        String firstName,

        @Schema(example = "أحمد")
        @Size(max = 100)
        String lastName,

        @Schema(example = "01012345678", description = "Egyptian mobile; normalized to E.164")
        String phone,

        @Schema(example = "mohammed@example.com")
        @Email(message = "Not a valid email address")
        @Size(max = 255)
        String email,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String password,

        @Schema(example = "ar", allowableValues = {"ar", "en"})
        String locale
) {
}
