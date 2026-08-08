package com.velora.api.identity.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Issues and validates JWT access tokens, and generates opaque refresh tokens.
 *
 * <p>Design notes:
 * <ul>
 *   <li>The <b>access token</b> is a signed JWT, short-lived (30 min), and carries
 *       the roles so no database hit is needed to authorize a request.</li>
 *   <li>The <b>refresh token</b> is NOT a JWT. It is a random opaque string stored
 *       as a SHA-256 hash, so it can be revoked server-side. A JWT cannot be
 *       revoked before it expires — which is exactly what "sign out everywhere"
 *       requires.</li>
 * </ul>
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_PHONE = "phone";
    private static final String ISSUER = "velora-api";

    private final SecretKey signingKey;
    private final long accessTokenMinutes;
    private final long refreshTokenDays;
    private final SecureRandom random = new SecureRandom();

    public JwtService(
            @Value("${velora.jwt.secret}") String secret,
            @Value("${velora.jwt.access-token-minutes}") long accessTokenMinutes,
            @Value("${velora.jwt.refresh-token-days}") long refreshTokenDays) {

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 64) {
            throw new IllegalStateException(
                    "velora.jwt.secret must be at least 64 characters for HS512 signing. "
                            + "Current length: " + keyBytes.length);
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenMinutes = accessTokenMinutes;
        this.refreshTokenDays = refreshTokenDays;
    }

    // ------------------------------------------------------------ access token

    public String generateAccessToken(Long userId, String email, String phone, Set<String> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plus(accessTokenMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(userId))
                .claim(CLAIM_ROLES, List.copyOf(roles))
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_PHONE, phone)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey, Jwts.SIG.HS512)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** @return true if the signature, issuer and expiry are all valid */
    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.debug("Expired JWT presented");
            return false;
        } catch (JwtException | IllegalArgumentException ex) {
            // Invalid signature, malformed token, wrong issuer — all equally rejected.
            log.debug("Invalid JWT presented: {}", ex.getMessage());
            return false;
        }
    }

    public Long extractUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Object roles = parseClaims(token).get(CLAIM_ROLES);
        return roles instanceof List<?> list ? (List<String>) list : List.of();
    }

    public long getAccessTokenSeconds() {
        return accessTokenMinutes * 60;
    }

    public long getRefreshTokenDays() {
        return refreshTokenDays;
    }

    // ----------------------------------------------------------- refresh token

    /** 512 bits of entropy, URL-safe. Opaque by design — it carries no claims. */
    public String generateRefreshToken() {
        byte[] bytes = new byte[64];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256, not BCrypt. The token already has 512 bits of entropy, so it needs
     * no salt or work factor — and refresh happens on every session, so a slow
     * hash would be pure latency.
     */
    public String hashToken(String rawToken) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
