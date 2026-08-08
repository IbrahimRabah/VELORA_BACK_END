package com.velora.api.inventory.domain;

public enum ReservationStatus {

    /** Stock is held. Counts against available quantity. */
    HELD,

    /** The order was created — the hold became a real commitment. */
    COMMITTED,

    /** Checkout failed or was abandoned; the units went back to available. */
    RELEASED,

    /** The hold timed out and a scheduled job returned the units. */
    EXPIRED
}
