package com.velora.api.identity.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET =
            "test_secret_that_is_definitely_long_enough_for_hs512_signing_0123456789";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 30, 30);
    }

    @Test
    void issuesAndParsesAnAccessToken() {
        String token = jwtService.generateAccessToken(
                42L, "a@b.com", "+201012345678", Set.of("CUSTOMER"));

        assertThat(jwtService.isValid(token)).isTrue();
        assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
        assertThat(jwtService.extractRoles(token)).containsExactly("CUSTOMER");
    }

    @Test
    @DisplayName("A token signed with a different key is rejected")
    void rejectsTokenSignedWithAnotherKey() {
        JwtService other = new JwtService(
                "a_completely_different_secret_also_long_enough_for_hs512_9876543210", 30, 30);
        String foreignToken = other.generateAccessToken(1L, null, null, Set.of("ADMIN"));

        assertThat(jwtService.isValid(foreignToken)).isFalse();
    }

    @Test
    void rejectsTamperedAndMalformedTokens() {
        String token = jwtService.generateAccessToken(1L, null, null, Set.of("CUSTOMER"));
        assertThat(jwtService.isValid(token + "x")).isFalse();
        assertThat(jwtService.isValid("not.a.token")).isFalse();
        assertThat(jwtService.isValid("")).isFalse();
    }

    @Test
    @DisplayName("An already-expired token is invalid")
    void rejectsExpiredToken() throws InterruptedException {
        JwtService shortLived = new JwtService(SECRET, 0, 30);
        String token = shortLived.generateAccessToken(1L, null, null, Set.of("CUSTOMER"));
        Thread.sleep(1100);
        assertThat(shortLived.isValid(token)).isFalse();
    }

    @Test
    @DisplayName("A short secret is refused at startup, not at first request")
    void refusesWeakSecret() {
        assertThatThrownBy(() -> new JwtService("too_short", 30, 30))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 64 characters");
    }

    @Test
    void refreshTokensAreUniqueAndHashStably() {
        String a = jwtService.generateRefreshToken();
        String b = jwtService.generateRefreshToken();

        assertThat(a).isNotEqualTo(b);
        assertThat(jwtService.hashToken(a)).isEqualTo(jwtService.hashToken(a));
        assertThat(jwtService.hashToken(a)).isNotEqualTo(jwtService.hashToken(b));
        // The stored hash must not reveal the token
        assertThat(jwtService.hashToken(a)).isNotEqualTo(a);
    }

    @Test
    void reportsAccessTokenLifetimeInSeconds() {
        assertThat(jwtService.getAccessTokenSeconds()).isEqualTo(1800);
    }
}
