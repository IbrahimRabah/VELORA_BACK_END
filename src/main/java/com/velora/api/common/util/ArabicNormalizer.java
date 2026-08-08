package com.velora.api.common.util;

import java.util.regex.Pattern;

/**
 * Normalizes Arabic text so that search matches the way customers actually type.
 *
 * <p>CRITICAL RULE: the SAME function must be applied when WRITING
 * {@code product_translation.search_text} and when READING (building the query).
 * If the two ever diverge, search silently stops matching.
 *
 * <p>Example: a customer typing {@code ساعه ذهبى} will match a product stored
 * as {@code ساعة ذهبي}, because both normalize to {@code ساعه ذهبي}.
 */
public final class ArabicNormalizer {

    private ArabicNormalizer() {
        // utility class
    }

    /** Tashkeel (diacritics) + superscript alef. */
    private static final Pattern TASHKEEL = Pattern.compile("[\u064B-\u065F\u0670]");

    /** Tatweel / kashida — the decorative stretching character. */
    private static final Pattern TATWEEL = Pattern.compile("\u0640");

    /** Collapse any run of whitespace into a single space. */
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    /**
     * Keeps only Arabic letters, Latin letters, digits and spaces.
     *
     * <p>Note this is an explicit allow-list rather than {@code \p{IsArabic}}: the
     * Arabic Unicode block also contains punctuation (، ؛ ؟ ٪) which must be
     * stripped, not kept.
     */
    private static final Pattern PUNCTUATION =
            Pattern.compile("[^\u0621-\u064Aa-zA-Z0-9 ]");

    public static String normalize(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        String s = input.trim().toLowerCase();

        s = TASHKEEL.matcher(s).replaceAll("");
        s = TATWEEL.matcher(s).replaceAll("");

        s = s.replace('\u0623', '\u0627')   // أ -> ا
             .replace('\u0625', '\u0627')   // إ -> ا
             .replace('\u0622', '\u0627')   // آ -> ا
             .replace('\u0671', '\u0627')   // ٱ -> ا
             .replace('\u0629', '\u0647')   // ة -> ه
             .replace('\u0649', '\u064A')   // ى -> ي
             .replace('\u0624', '\u0648')   // ؤ -> و
             .replace('\u0626', '\u064A');  // ئ -> ي

        s = convertArabicDigits(s);
        s = PUNCTUATION.matcher(s).replaceAll(" ");
        s = WHITESPACE.matcher(s).replaceAll(" ").trim();

        return s.isEmpty() ? null : s;
    }

    /** Converts Arabic-Indic (٠-٩) and Extended Arabic-Indic (۰-۹) digits to 0-9. */
    public static String convertArabicDigits(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            if (c >= '\u0660' && c <= '\u0669') {
                sb.append((char) (c - '\u0660' + '0'));
            } else if (c >= '\u06F0' && c <= '\u06F9') {
                sb.append((char) (c - '\u06F0' + '0'));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Builds a SQL LIKE pattern from a user query, e.g. {@code %ساعه%ذهبي%}. */
    public static String toLikePattern(String query) {
        String normalized = normalize(query);
        if (normalized == null) {
            return "%";
        }
        return "%" + normalized.replace(" ", "%") + "%";
    }
}
