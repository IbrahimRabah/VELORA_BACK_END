package com.velora.api.order.domain;

/** Which of the two state machines a history entry belongs to. */
public enum StatusKind {
    FULFILLMENT,
    PAYMENT
}
