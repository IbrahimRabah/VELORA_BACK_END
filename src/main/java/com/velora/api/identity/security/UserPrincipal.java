package com.velora.api.identity.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * The authenticated caller, built from the JWT — no database lookup per request.
 *
 * <p>Inject it in a controller with {@code @AuthenticationPrincipal UserPrincipal me}.
 */
public record UserPrincipal(
        Long id,
        String email,
        String phone,
        Collection<? extends GrantedAuthority> authorities
) {

    public static UserPrincipal of(Long id, String email, String phone, List<String> roles) {
        List<SimpleGrantedAuthority> authorities = roles.stream()
                // Spring's hasRole("ADMIN") checks for the authority "ROLE_ADMIN".
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        return new UserPrincipal(id, email, phone, authorities);
    }

    public boolean hasRole(String role) {
        return authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
}
