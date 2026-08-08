package com.velora.api.common.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Generates URL slugs for products, categories and CMS pages.
 *
 * <p>Prefer the English name as the source. Percent-encoded Arabic in a URL is ugly
 * to share and awkward in analytics, so Arabic input is transliterated to Latin.
 *
 * <p>Once a slug is published it should be treated as stable. If it must change,
 * write the old value into {@code url_redirect} so existing links and search
 * rankings are not lost.
 */
public final class SlugGenerator {

    private SlugGenerator() {
        // utility class
    }

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w\\s-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private static final Pattern MULTI_DASH = Pattern.compile("-{2,}");
    private static final Pattern EDGE_DASH = Pattern.compile("^-+|-+$");

    private static final int MAX_LENGTH = 180;

    public static String generate(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        String s = transliterateArabic(input.trim());
        s = Normalizer.normalize(s, Normalizer.Form.NFD);
        s = s.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        s = s.toLowerCase(Locale.ENGLISH);
        s = NON_LATIN.matcher(s).replaceAll("-");
        s = WHITESPACE.matcher(s).replaceAll("-");
        s = s.replace('_', '-');
        s = MULTI_DASH.matcher(s).replaceAll("-");
        s = EDGE_DASH.matcher(s).replaceAll("");

        if (s.length() > MAX_LENGTH) {
            s = s.substring(0, MAX_LENGTH);
            s = EDGE_DASH.matcher(s).replaceAll("");
        }
        return s.isEmpty() ? null : s;
    }

    /**
     * Appends {@code -2}, {@code -3} … until {@code isAvailable} accepts the result.
     * Pass a lambda that checks the repository, e.g.
     * {@code slug -> !productRepository.existsBySlug(slug)}.
     */
    public static String generateUnique(String input, Predicate<String> isAvailable) {
        String base = generate(input);
        if (base == null) {
            return null;
        }
        if (isAvailable.test(base)) {
            return base;
        }
        for (int suffix = 2; suffix < 1000; suffix++) {
            String candidate = base + "-" + suffix;
            if (isAvailable.test(candidate)) {
                return candidate;
            }
        }
        return base + "-" + System.currentTimeMillis();
    }

    private static final String[][] ARABIC_MAP = {
            {"ا", "a"}, {"أ", "a"}, {"إ", "a"}, {"آ", "a"}, {"ٱ", "a"},
            {"ب", "b"}, {"ت", "t"}, {"ث", "th"}, {"ج", "g"}, {"ح", "h"},
            {"خ", "kh"}, {"د", "d"}, {"ذ", "th"}, {"ر", "r"}, {"ز", "z"},
            {"س", "s"}, {"ش", "sh"}, {"ص", "s"}, {"ض", "d"}, {"ط", "t"},
            {"ظ", "z"}, {"ع", "a"}, {"غ", "gh"}, {"ف", "f"}, {"ق", "q"},
            {"ك", "k"}, {"ل", "l"}, {"م", "m"}, {"ن", "n"}, {"ه", "h"},
            {"ة", "a"}, {"و", "w"}, {"ؤ", "w"}, {"ي", "y"}, {"ى", "a"},
            {"ئ", "y"}, {"ء", ""}, {"ـ", ""}
    };

    private static String transliterateArabic(String input) {
        String s = ArabicNormalizer.convertArabicDigits(input);
        s = s.replaceAll("[\u064B-\u065F\u0670]", "");
        for (String[] pair : ARABIC_MAP) {
            s = s.replace(pair[0], pair[1]);
        }
        return s;
    }
}
