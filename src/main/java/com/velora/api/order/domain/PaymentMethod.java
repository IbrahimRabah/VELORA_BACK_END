package com.velora.api.order.domain;

public enum PaymentMethod {

    /** The only method in V1, and the majority of orders in this market. */
    COD,

    CARD,
    WALLET,
    FAWRY
}
