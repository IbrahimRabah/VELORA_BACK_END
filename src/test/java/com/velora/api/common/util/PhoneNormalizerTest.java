package com.velora.api.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PhoneNormalizerTest {

    @ParameterizedTest
    @DisplayName("Every way a customer might type the same number normalizes identically")
    @ValueSource(strings = {
            "01012345678",
            "+201012345678",
            "00201012345678",
            "201012345678",
            "0101 234 5678",
            "0101-234-5678",
            "  01012345678  ",
            "١٠١٢٣٤٥٦٧٨"
    })
    void normalizesAllInputForms(String input) {
        assertThat(PhoneNormalizer.toE164(input)).isEqualTo("+201012345678");
    }

    @ParameterizedTest
    @DisplayName("All four Egyptian mobile prefixes are accepted")
    @ValueSource(strings = {"01012345678", "01112345678", "01212345678", "01512345678"})
    void acceptsAllValidPrefixes(String input) {
        assertThat(PhoneNormalizer.isValid(input)).isTrue();
    }

    @ParameterizedTest
    @DisplayName("Invalid numbers are rejected rather than silently mangled")
    @ValueSource(strings = {
            "0131234567",      // invalid prefix 013
            "0101234567",      // too short
            "010123456789",    // too long
            "123",
            "not a phone",
            "+11234567890"     // not Egyptian
    })
    void rejectsInvalidNumbers(String input) {
        assertThat(PhoneNormalizer.toE164(input)).isNull();
        assertThat(PhoneNormalizer.isValid(input)).isFalse();
    }

    @Test
    void handlesNullAndBlank() {
        assertThat(PhoneNormalizer.toE164(null)).isNull();
        assertThat(PhoneNormalizer.toE164("")).isNull();
        assertThat(PhoneNormalizer.toE164("   ")).isNull();
    }

    @Test
    void convertsBackToLocalFormatForCourierLabels() {
        assertThat(PhoneNormalizer.toLocalFormat("+201012345678")).isEqualTo("01012345678");
        assertThat(PhoneNormalizer.toLocalFormat("01012345678")).isEqualTo("01012345678");
    }

    @Test
    void masksForSupportScreensAndLogs() {
        assertThat(PhoneNormalizer.mask("+201012345678")).isEqualTo("0101****678");
    }

    @Test
    @DisplayName("Normalization is idempotent — safe to apply on write and on lookup")
    void isIdempotent() {
        String once = PhoneNormalizer.toE164("01012345678");
        assertThat(PhoneNormalizer.toE164(once)).isEqualTo(once);
    }
}
