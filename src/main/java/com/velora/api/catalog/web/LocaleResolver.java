package com.velora.api.catalog.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;

/**
 * Resolves the response language from {@code Accept-Language}.
 *
 * <p>Falls back to Arabic — the primary market — rather than to English or to the
 * server default, and never returns a locale the catalog has no translations for.
 */
public final class LocaleResolver {

    private LocaleResolver() {
        // utility class
    }

    public static final String DEFAULT_LOCALE = "ar";
    private static final Set<String> SUPPORTED = Set.of("ar", "en");

    public static String resolve(HttpServletRequest request) {
        String header = request.getHeader("Accept-Language");
        if (header == null || header.isBlank()) {
            return DEFAULT_LOCALE;
        }
        // "en-US,en;q=0.9,ar;q=0.8" -> "en"
        String primary = header.split(",")[0].split("-")[0].trim().toLowerCase();
        return SUPPORTED.contains(primary) ? primary : DEFAULT_LOCALE;
    }
}
