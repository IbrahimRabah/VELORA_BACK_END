package com.velora.api.cart.security;

import com.velora.api.common.exception.BusinessException;
import com.velora.api.common.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Issues and verifies the {@code X-Guest-Token} that identifies an anonymous
 * shopper's cart.
 *
 * <p>Before this existed, the token was a bare UUID the Angular app generated
 * itself and the server trusted verbatim. Anyone who learned another guest's token
 * — a shared screen, a logged proxy, a leaked referrer — could read and edit that
 * person's cart, delivery address and phone number included. The server now issues
 * the token and signs it, so a client cannot forge one for an id it does not
 * already hold.
 *
 * <p>Format: {@code <uuid>.<base64url HMAC-SHA256 of the uuid>}. Neither the UUID
 * nor a base64url signature can contain a '.', so splitting on the last one is
 * unambiguous.
 *
 * <p>{@code acceptLegacyUnsigned} is a transition switch. Carts created before this
 * change carry a bare, unsigned UUID. Rejecting them outright would lock every one
 * of those shoppers out of their own cart. While the flag is on, an unsigned token
 * is still accepted — but logged at WARN — so traffic can be watched until it is
 * safe to flip the flag off and require a signature from everyone.
 */
@Service
public class GuestTokenService {

    private static final Logger log = LoggerFactory.getLogger(GuestTokenService.class);
    private static final String ALGORITHM = "HmacSHA256";
    private static final char SEPARATOR = '.';

    private final SecretKeySpec keySpec;
    private final boolean acceptLegacyUnsigned;

    public GuestTokenService(
            @Value("${velora.guest-token.secret}") String secret,
            @Value("${velora.guest-token.accept-legacy-unsigned:false}")
            boolean acceptLegacyUnsigned) {

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "velora.guest-token.secret must be at least 32 characters for "
                            + "HmacSHA256 signing. Current length: " + keyBytes.length);
        }
        this.keySpec = new SecretKeySpec(keyBytes, ALGORITHM);
        this.acceptLegacyUnsigned = acceptLegacyUnsigned;
    }

    /** A fresh, signed token for a new anonymous cart. */
    public String generate() {
        String uuid = UUID.randomUUID().toString();
        return uuid + SEPARATOR + sign(uuid);
    }

    /**
     * Verifies a token presented as {@code X-Guest-Token} (or in the body of
     * {@code POST /api/v1/cart/merge}).
     *
     * <p>A blank token is not this method's concern — callers that require a guest
     * identity already reject a missing one with their own error ({@code CART_EMPTY}
     * or {@code VALIDATION_FAILED}). This only judges tokens that are actually
     * present.
     *
     * @throws BusinessException with {@link ErrorCode#TOKEN_INVALID} if a signature
     *         is present and wrong, or absent while {@code acceptLegacyUnsigned} is off
     */
    public void verify(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        int sep = token.lastIndexOf(SEPARATOR);
        if (sep < 0) {
            if (acceptLegacyUnsigned) {
                log.warn("Accepted an unsigned legacy guest token ({}...). Set "
                                + "velora.guest-token.accept-legacy-unsigned=false once old "
                                + "carts have had time to expire.",
                        token.substring(0, Math.min(8, token.length())));
                return;
            }
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "Guest token is not signed");
        }

        String uuid = token.substring(0, sep);
        String presentedSignature = token.substring(sep + 1);
        String expectedSignature = sign(uuid);

        // Constant-time comparison: a signature check that returns faster on an
        // early mismatched byte leaks how much of the guess was already correct.
        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                presentedSignature.getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID,
                    "Guest token signature is invalid");
        }
    }

    private String sign(String uuid) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(keySpec);
            byte[] raw = mac.doFinal(uuid.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(ALGORITHM + " is not available", ex);
        }
    }
}
