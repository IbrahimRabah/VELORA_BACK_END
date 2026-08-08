package com.velora.api.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables {@code @Scheduled}. Currently drives the reservation cleanup; later the
 * COD reconciliation and abandoned-cart jobs.
 *
 * <p>Note this runs in every instance. When VELORA moves to more than one server,
 * these jobs need a lock (ShedLock or similar) so two instances do not do the same
 * work at the same time.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
