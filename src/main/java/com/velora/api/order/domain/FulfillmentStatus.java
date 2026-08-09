package com.velora.api.order.domain;

/**
 * Where the goods are. Completely independent of whether money has been received.
 *
 * <p>The states after SHIPPED are the ones a naive design forgets, and they are the
 * common ones in this market: the customer does not answer, changes their mind at
 * the door, or refuses to pay. Each needs a name, because each needs different
 * handling.
 */
public enum FulfillmentStatus {

    /** Created. Stock is reserved, awaiting confirmation. */
    PENDING,

    /** Accepted by the store — usually after a phone call for COD. */
    CONFIRMED,

    /** Being picked and packed. */
    PROCESSING,

    /** Handed to the courier. Stock is committed at this point. */
    SHIPPED,

    /** With the courier for final delivery. */
    OUT_FOR_DELIVERY,

    /** Received by the customer. Starts the return window. */
    DELIVERED,

    /** Attempted and not completed. Carries an attempt counter and a reason. */
    DELIVERY_FAILED,

    /** The customer declined to accept or to pay. Common with COD. */
    REFUSED_ON_DELIVERY,

    /** The goods came back after a failed or refused delivery. */
    RETURNED_TO_SELLER,

    /** Terminated before shipment. */
    CANCELLED,

    /** Fully returned after delivery. */
    RETURNED,

    /** Some lines returned, some kept. */
    PARTIALLY_RETURNED;

    /** No further movement is possible from here. */
    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED
                || this == RETURNED || this == RETURNED_TO_SELLER;
    }

    /** Stock has left the building. */
    public boolean isDispatched() {
        return this == SHIPPED || this == OUT_FOR_DELIVERY || this == DELIVERED
                || this == DELIVERY_FAILED || this == REFUSED_ON_DELIVERY;
    }
}
