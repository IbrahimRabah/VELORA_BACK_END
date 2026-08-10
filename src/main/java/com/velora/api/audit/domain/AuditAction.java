package com.velora.api.audit.domain;

/**
 * What was done. Deliberately coarse.
 *
 * <p>An audit log is read months later by someone asking a specific question — "who
 * dropped that price?", "where did those six units go?". A hundred fine-grained
 * action types make that harder, not easier.
 */
public enum AuditAction {

    /** A price or cost changed. The most disputed field in any catalog. */
    PRICE_CHANGED,

    /** Stock moved by hand rather than through an order. */
    STOCK_ADJUSTED,

    STOCK_RECEIVED,

    /** A product went on or off sale. */
    PRODUCT_PUBLISHED,
    PRODUCT_ARCHIVED,

    /** Shipping rates changed — affects what every future customer pays. */
    SHIPPING_RATE_CHANGED,

    /** An invoice was voided. Always needs a reason. */
    INVOICE_CANCELLED,

    /** A refund or a manual payment status change. */
    PAYMENT_ADJUSTED,

    /** Roles granted or revoked. */
    PERMISSION_CHANGED,

    /** Seller legal details changed — these end up printed on invoices. */
    STORE_PROFILE_CHANGED
}
