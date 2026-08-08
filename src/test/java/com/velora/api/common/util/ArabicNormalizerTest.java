package com.velora.api.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ArabicNormalizerTest {

    @ParameterizedTest
    @DisplayName("All alef forms collapse to bare alef")
    @ValueSource(strings = {"أحمد", "إحمد", "آحمد", "ٱحمد", "احمد"})
    void normalizesAlefForms(String input) {
        assertThat(ArabicNormalizer.normalize(input)).isEqualTo("احمد");
    }

    @Test
    @DisplayName("Ta marbuta becomes ha — ساعة and ساعه must match")
    void normalizesTaMarbuta() {
        assertThat(ArabicNormalizer.normalize("ساعة"))
                .isEqualTo(ArabicNormalizer.normalize("ساعه"));
    }

    @Test
    @DisplayName("Alef maqsura becomes ya — ذهبى and ذهبي must match")
    void normalizesYaForms() {
        assertThat(ArabicNormalizer.normalize("ذهبى"))
                .isEqualTo(ArabicNormalizer.normalize("ذهبي"));
    }

    @Test
    @DisplayName("Diacritics are stripped")
    void stripsTashkeel() {
        assertThat(ArabicNormalizer.normalize("سَاعَةٌ"))
                .isEqualTo(ArabicNormalizer.normalize("ساعة"));
    }

    @Test
    @DisplayName("Tatweel is stripped")
    void stripsTatweel() {
        assertThat(ArabicNormalizer.normalize("ســـاعة"))
                .isEqualTo(ArabicNormalizer.normalize("ساعة"));
    }

    @ParameterizedTest
    @DisplayName("Arabic-Indic digits become Western digits")
    @CsvSource({"٤٢, 42", "١٢٣٤٥, 12345", "٠, 0"})
    void convertsArabicDigits(String input, String expected) {
        assertThat(ArabicNormalizer.convertArabicDigits(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("The realistic case: how a customer types vs how it is stored")
    void customerTypingMatchesStoredText() {
        String stored = ArabicNormalizer.normalize("سَاعَة رِجَالِي ذَهَبِيَّة");
        String typed = ArabicNormalizer.normalize("ساعه رجالى ذهبيه");
        assertThat(typed).isEqualTo(stored);
    }

    @Test
    void collapsesWhitespaceAndTrims() {
        assertThat(ArabicNormalizer.normalize("  ساعة    ذهبية  ")).isEqualTo("ساعه ذهبيه");
    }

    @Test
    void lowercasesLatinText() {
        assertThat(ArabicNormalizer.normalize("VELORA Watch")).isEqualTo("velora watch");
    }

    @Test
    void stripsPunctuation() {
        assertThat(ArabicNormalizer.normalize("ساعة، ذهبية!")).isEqualTo("ساعه ذهبيه");
    }

    @Test
    void returnsNullForEmptyInput() {
        assertThat(ArabicNormalizer.normalize(null)).isNull();
        assertThat(ArabicNormalizer.normalize("")).isNull();
        assertThat(ArabicNormalizer.normalize("   ")).isNull();
    }

    @Test
    void buildsLikePatternForSql() {
        assertThat(ArabicNormalizer.toLikePattern("ساعة ذهبية")).isEqualTo("%ساعه%ذهبيه%");
        assertThat(ArabicNormalizer.toLikePattern(null)).isEqualTo("%");
    }

    @Test
    @DisplayName("Normalization is idempotent — running it twice changes nothing")
    void isIdempotent() {
        String once = ArabicNormalizer.normalize("سَاعَة رِجَالِي");
        assertThat(ArabicNormalizer.normalize(once)).isEqualTo(once);
    }
}
