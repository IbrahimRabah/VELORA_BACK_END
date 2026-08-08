package com.velora.api.cart.domain;

public enum CartStatus {

    /** In use. Exactly one active cart per user, and one per guest token. */
    ACTIVE,

    /** Folded into an account cart when a guest signed in. */
    MERGED,

    /** Turned into an order. */
    CONVERTED,

    /** Untouched long enough to be considered abandoned. */
    ABANDONED
}
