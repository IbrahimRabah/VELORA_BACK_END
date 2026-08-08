package com.velora.api.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SlugGeneratorTest {

    @Test
    void generatesFromEnglishName() {
        assertThat(SlugGenerator.generate("Classic Gold Watch 42mm"))
                .isEqualTo("classic-gold-watch-42mm");
    }

    @Test
    void collapsesPunctuationAndRepeatedDashes() {
        assertThat(SlugGenerator.generate("VELORA -- Men's  Wallet!!"))
                .isEqualTo("velora-men-s-wallet");
    }

    @Test
    @DisplayName("Arabic is transliterated rather than percent-encoded in the URL")
    void transliteratesArabic() {
        String slug = SlugGenerator.generate("ساعة ذهبية");
        assertThat(slug).isNotNull();
        assertThat(slug).matches("[a-z0-9-]+");
    }

    @Test
    void hasNoLeadingOrTrailingDashes() {
        assertThat(SlugGenerator.generate("  --Gold Watch--  ")).isEqualTo("gold-watch");
    }

    @Test
    void returnsNullForEmptyInput() {
        assertThat(SlugGenerator.generate(null)).isNull();
        assertThat(SlugGenerator.generate("   ")).isNull();
    }

    @Test
    void appendsSuffixUntilUnique() {
        Set<String> taken = Set.of("gold-watch", "gold-watch-2");
        String slug = SlugGenerator.generateUnique("Gold Watch", s -> !taken.contains(s));
        assertThat(slug).isEqualTo("gold-watch-3");
    }

    @Test
    void keepsBaseSlugWhenAvailable() {
        String slug = SlugGenerator.generateUnique("Gold Watch", s -> true);
        assertThat(slug).isEqualTo("gold-watch");
    }
}
