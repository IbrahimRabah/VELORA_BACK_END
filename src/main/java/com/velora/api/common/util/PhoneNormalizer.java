package com.velora.api.common.util;

/**
 * Normalizes Egyptian mobile numbers to E.164 ({@code +201012345678}).
 *
 * <p>The phone number is a login identifier, so it MUST be normalized both before
 * storing and before looking up. Skip it on one side and {@code 01012345678} and
 * {@code +201012345678} become two different accounts for the same person.
 *
 * <p>Accepted inputs: {@code 01012345678}, {@code +201012345678}, {@code 00201012345678},
 * {@code 201012345678}, {@code ٠١٠١٢٣٤٥٦٧٨} — with or without spaces and dashes.
 */
public final class PhoneNormalizer {

    private PhoneNormalizer() {
        // utility class
    }

    private static final String EGYPT_CODE = "20";

    /** Valid Egyptian mobile prefixes after the country code: 010, 011, 012, 015. */
    private static final String[] VALID_PREFIXES = {"10", "11", "12", "15"};

    /**
     * @return the number in E.164 form, or {@code null} if it is not a valid
     *         Egyptian mobile number
     */
    public static String toE164(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        String digits = ArabicNormalizer.convertArabicDigits(input.trim())
                .replaceAll("[^0-9+]", "");

        if (digits.startsWith("+")) {
            digits = digits.substring(1);
        }
        if (digits.startsWith("00")) {
            digits = digits.substring(2);
        }

        // Local form: 01012345678 -> 201012345678
        if (digits.startsWith("0") && digits.length() == 11) {
            digits = EGYPT_CODE + digits.substring(1);
        }

        // Bare form: 1012345678 -> 201012345678
        if (digits.length() == 10 && isValidPrefix(digits.substring(0, 2))) {
            digits = EGYPT_CODE + digits;
        }

        if (!digits.startsWith(EGYPT_CODE) || digits.length() != 12) {
            return null;
        }
        if (!isValidPrefix(digits.substring(2, 4))) {
            return null;
        }

        return "+" + digits;
    }

    public static boolean isValid(String input) {
        return toE164(input) != null;
    }

    /** {@code +201012345678} -> {@code 01012345678}, for display and courier labels. */
    public static String toLocalFormat(String e164) {
        String normalized = toE164(e164);
        if (normalized == null) {
            return null;
        }
        return "0" + normalized.substring(3);
    }

    /** {@code +201012345678} -> {@code 0101****678}, for support screens and logs. */
    public static String mask(String e164) {
        String local = toLocalFormat(e164);
        if (local == null) {
            return null;
        }
        return local.substring(0, 4) + "****" + local.substring(8);
    }

    private static boolean isValidPrefix(String prefix) {
        for (String valid : VALID_PREFIXES) {
            if (valid.equals(prefix)) {
                return true;
            }
        }
        return false;
    }
}
