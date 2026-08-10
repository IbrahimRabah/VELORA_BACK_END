package com.velora.api.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Who VELORA is, legally. One row, id = 1.
 *
 * <p>A dedicated table rather than key/value rows in {@code store_setting}: the
 * seller's identity is a fixed, known set of fields, so typed columns and validation
 * beat a bag of strings.
 *
 * <p>Editable through the API because a legal address changes, and registering for
 * tax should not require a redeploy. Every field here is also COPIED onto each
 * invoice — an invoice must always show the details that were printed on it, not
 * whatever the store profile says today.
 */
@Entity
@Table(name = "store_profile")
@Getter
@Setter
@NoArgsConstructor
public class StoreProfile {

    @Id
    @Column(name = "id")
    private Integer id = 1;

    @Column(name = "legal_name", nullable = false, length = 200)
    private String legalName;

    @Column(name = "legal_name_en", length = 200)
    private String legalNameEn;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    /**
     * Egyptian tax registration number, normally nine digits as 123-456-789.
     *
     * <p>Left null until registration. A blank line is omitted from the invoice
     * entirely — printing a wrong number on a legal document is far worse than
     * printing none.
     */
    @Column(name = "tax_number", length = 30)
    private String taxNumber;

    @Column(name = "commercial_register", length = 30)
    private String commercialRegister;

    @Column(name = "website", length = 255)
    private String website;

    @Column(name = "invoice_footer_note", length = 500)
    private String invoiceFooterNote;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now(ZoneOffset.UTC);

    public boolean hasTaxNumber() {
        return taxNumber != null && !taxNumber.isBlank();
    }

    public boolean hasCommercialRegister() {
        return commercialRegister != null && !commercialRegister.isBlank();
    }
}
