package com.velora.api.idempotency.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Removes idempotency records past their window.
 *
 * <p>A key only matters while a retry is plausible. Without this the table grows
 * forever, and it stores full response bodies — the row for one order is not small.
 *
 * <p>Runs nightly because nothing depends on it being prompt.
 */
@Component
public class IdempotencyCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyCleanupJob.class);

    private final IdempotencyService idempotencyService;

    public IdempotencyCleanupJob(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    @Scheduled(cron = "0 30 3 * * *")
    public void purge() {
        try {
            int removed = idempotencyService.purgeExpired();
            if (removed > 0) {
                log.info("Purged {} expired idempotency record(s)", removed);
            }
        } catch (Exception ex) {
            // Housekeeping must never take down the scheduler thread.
            log.error("Idempotency purge failed", ex);
        }
    }
}
