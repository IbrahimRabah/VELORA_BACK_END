package com.velora.api.inventory.domain;

/**
 * Why stock changed. Every quantity change writes one of these — the ledger is
 * append-only, so "how did we get to 3 units?" always has an answer.
 */
public enum MovementType {

    /** Goods received from a supplier. */
    PURCHASE_RECEIVED,

    /** Order shipped — reservation becomes a real reduction. */
    SALE,

    /** Order cancelled before shipment. */
    CANCELLATION_RESTOCK,

    /** Return passed inspection and goes back on sale. */
    RETURN_SELLABLE,

    /** Return failed inspection — quarantined, NOT resold. */
    RETURN_DAMAGED,

    /** Breakage, theft or loss recorded by staff. */
    DAMAGE_WRITEOFF,

    /** Physical count correction. Always requires a reason and is audited. */
    MANUAL_ADJUSTMENT
}
