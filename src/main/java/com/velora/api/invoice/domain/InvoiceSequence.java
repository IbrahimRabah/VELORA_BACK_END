package com.velora.api.invoice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One counter row per fiscal year.
 *
 * <p>The counter is NOT an identity column, and that is deliberate. A database
 * identity leaves gaps whenever a transaction rolls back — perfectly fine for an
 * order id, unacceptable for an invoice number. Here the number is allocated under a
 * row lock inside the same transaction that writes the invoice, so a rollback
 * returns the number rather than burning it.
 *
 * <p>The trade-off is that invoice issuance serialises. At this volume that costs
 * nothing, and correctness is not negotiable for a legal document.
 */
@Entity
@Table(name = "invoice_sequence")
@Getter
@Setter
@NoArgsConstructor
public class InvoiceSequence {

    @Id
    @Column(name = "fiscal_year")
    private Integer fiscalYear;

    @Column(name = "last_number", nullable = false)
    private int lastNumber;

    public InvoiceSequence(Integer fiscalYear) {
        this.fiscalYear = fiscalYear;
        this.lastNumber = 0;
    }

    public int next() {
        return ++lastNumber;
    }
}
