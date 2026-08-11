package com.velora.api.cart.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.velora.api.common.exception.BusinessException;
import com.velora.api.common.exception.ErrorCode;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GuestTokenServiceTest {

    private static final String SECRET =
            "test_secret_that_is_definitely_long_enough_for_hmac_sha256_signing";

    @Test
    @DisplayName("A freshly generated, signed token verifies")
    void signedTokenVerifies() {
        GuestTokenService service = new GuestTokenService(SECRET, false);
        String token = service.generate();

        assertThatCode(() -> service.verify(token)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("A token with a tampered signature is rejected")
    void tamperedSignatureIsRejected() {
        GuestTokenService service = new GuestTokenService(SECRET, false);
        String token = service.generate();
        char lastChar = token.charAt(token.length() - 1);
        String tampered = token.substring(0, token.length() - 1)
                + (lastChar == 'a' ? 'b' : 'a');

        assertThatThrownBy(() -> service.verify(tampered))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.TOKEN_INVALID));
    }

    @Test
    @DisplayName("A token signed with a different key is rejected")
    void tokenFromAnotherKeyIsRejected() {
        GuestTokenService service = new GuestTokenService(SECRET, false);
        GuestTokenService other = new GuestTokenService(
                "a_completely_different_secret_also_long_enough_0123456789", false);
        String foreignToken = other.generate();

        assertThatThrownBy(() -> service.verify(foreignToken))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.TOKEN_INVALID));
    }

    @Test
    @DisplayName("A legacy, unsigned token is accepted while the transition flag is on")
    void legacyTokenAcceptedWhenFlagOn() {
        GuestTokenService service = new GuestTokenService(SECRET, true);
        String legacyToken = UUID.randomUUID().toString();

        assertThatCode(() -> service.verify(legacyToken)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("A legacy, unsigned token is rejected once the transition flag is off")
    void legacyTokenRejectedWhenFlagOff() {
        GuestTokenService service = new GuestTokenService(SECRET, false);
        String legacyToken = UUID.randomUUID().toString();

        assertThatThrownBy(() -> service.verify(legacyToken))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.TOKEN_INVALID));
    }

    @Test
    @DisplayName("A missing or blank token is not this service's concern")
    void blankTokenIsIgnored() {
        GuestTokenService service = new GuestTokenService(SECRET, false);

        assertThatCode(() -> service.verify(null)).doesNotThrowAnyException();
        assertThatCode(() -> service.verify("")).doesNotThrowAnyException();
        assertThatCode(() -> service.verify("   ")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Each generated token carries a fresh, unique uuid")
    void generatedTokensAreUnique() {
        GuestTokenService service = new GuestTokenService(SECRET, false);
        assertThat(service.generate()).isNotEqualTo(service.generate());
    }

    @Test
    @DisplayName("A weak secret is refused at startup, not at first request")
    void refusesWeakSecret() {
        assertThatThrownBy(() -> new GuestTokenService("too_short", false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 characters");
    }
}
