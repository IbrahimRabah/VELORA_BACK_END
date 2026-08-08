package com.velora.api.identity.mapper;

import com.velora.api.identity.domain.AppUser;
import com.velora.api.identity.domain.Role;
import com.velora.api.identity.dto.UserResponse;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Entity to DTO. Written by hand rather than generated because the mapping is
 * small and the security-relevant part — never exposing {@code passwordHash} — is
 * clearer when it is explicit.
 */
@Component
public class UserMapper {

    public UserResponse toResponse(AppUser user) {
        Set<String> roles = user.getRoles().stream()
                .map(Role::getCode)
                .collect(Collectors.toSet());

        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneE164(),
                user.isEmailVerified(),
                user.isPhoneVerified(),
                user.getPreferredLocale(),
                roles);
    }
}
