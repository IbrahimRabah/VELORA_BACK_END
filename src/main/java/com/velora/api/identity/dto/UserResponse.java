package com.velora.api.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;

/**
 * The safe view of a user. Never exposes the password hash — which is exactly why
 * controllers return DTOs and not entities.
 */
@Schema(description = "Signed-in user")
public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String fullName,
        String email,
        String phone,
        boolean emailVerified,
        boolean phoneVerified,
        String locale,
        Set<String> roles
) {
}
