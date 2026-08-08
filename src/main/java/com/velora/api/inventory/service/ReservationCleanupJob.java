package com.velora.api.inventory.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Returns timed-out holds to available stock.
 *
 * <p>Without this, one abandoned checkout freezes a unit forever and the product
 * eventually shows as out of stock while sitting on the shelf.
 *
 * <p>Runs every minute. The TTL is 20 minutes, so a minute of lag costs nothing and
 * the batch limit keeps a recovery after downtime from locking the table.
 */
@Component
public class ReservationCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(ReservationCleanupJob.class);
    private static final int BATCH_SIZE = 200;

    private final ReservationService reservationService;

    public ReservationCleanupJob(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT30S")
    public void releaseExpiredReservations() {
        try {
            int released = reservationService.releaseExpired(BATCH_SIZE);
            if (released > 0) {
                log.info("Released {} expired stock reservation(s)", released);
            }
            if (released == BATCH_SIZE) {
                // More than one batch was waiting — usually a backlog after an
                // outage, occasionally a payment integration that stopped
                // confirming. Worth noticing either way.
                log.warn("Expired-reservation backlog: a full batch of {} was released. "
                        + "{} still pending", BATCH_SIZE, reservationService.countExpiredHolds());
            }
        } catch (Exception ex) {
            // A failing cleanup must never stop the scheduler thread.
            log.error("Reservation cleanup failed", ex);
        }
    }
}
