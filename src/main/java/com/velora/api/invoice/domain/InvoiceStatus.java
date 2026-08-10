package com.velora.api.invoice.domain;

/**
 * An invoice is never deleted and never renumbered.
 *
 * <p>Both would leave a gap in the sequence, and a gapless sequence is the whole
 * reason the numbering is separate from the order number.
 */
public enum InvoiceStatus {

    /** Valid and countable. */
    ISSUED,

    /**
     * Voided. The number stays consumed and the row stays in place — a cancelled
     * invoice still has to be explainable to an auditor.
     */
    CANCELLED
}
