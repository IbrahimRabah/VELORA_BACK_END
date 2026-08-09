package com.velora.api.order.domain;

/**
 * Where the money is. A separate machine from fulfilment, on purpose.
 *
 * <p>{@code PENDING} on a delivered COD order is not a bug — it is the normal state
 * until the courier remits the cash. Collapsing the two into one status field makes
 * that combination inexpressible, which is why the schema keeps two columns.
 */
public enum PaymentStatus {

    /** No money received. The normal state for a COD order in transit. */
    PENDING,

    /** Card authorised but not captured. Unused until a gateway is added. */
    AUTHORIZED,

    /** Funds received and settled. */
    PAID,

    /** Part of the value returned to the customer. */
    PARTIALLY_REFUNDED,

    /** Full value returned. */
    REFUNDED,

    /** The payment attempt was rejected. */
    FAILED,

    /** The payment window elapsed. The reservation was released. */
    EXPIRED;

    public boolean isSettled() {
        return this == PAID || this == PARTIALLY_REFUNDED || this == REFUNDED;
    }
}
