package com.velora.api.common.config;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Enables {@code @CreatedDate} / {@code @LastModifiedDate} and resolves who made
 * the change for audited entities.
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "utcDateTimeProvider")
public class JpaAuditingConfig {

    /** All audit timestamps are UTC, regardless of the server's timezone. */
    @Bean
    public DateTimeProvider utcDateTimeProvider() {
        return () -> Optional.<TemporalAccessor>of(OffsetDateTime.now(ZoneOffset.UTC));
    }

    /**
     * Resolves the current user id for {@code @CreatedBy} fields.
     * Returns empty for anonymous traffic such as guest checkout.
     */
    @Bean
    public AuditorAware<Long> auditorAware() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return Optional.empty();
            }
            if ("anonymousUser".equals(String.valueOf(auth.getPrincipal()))) {
                return Optional.empty();
            }
            try {
                return Optional.of(Long.valueOf(auth.getName()));
            } catch (NumberFormatException ex) {
                // The JWT subject is not a numeric user id yet; the identity module sets this.
                return Optional.empty();
            }
        };
    }
}
