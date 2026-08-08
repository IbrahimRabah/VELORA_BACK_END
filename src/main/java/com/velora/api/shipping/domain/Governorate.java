package com.velora.api.shipping.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One of Egypt's 27 governorates.
 *
 * <p>This is the primary key of the whole shipping model. Postal codes are not
 * reliably used here and cannot route a parcel, so the governorate is what decides
 * the rate and the delivery estimate.
 */
@Entity
@Table(name = "governorate")
@Getter
@Setter
@NoArgsConstructor
public class Governorate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** CAI, GIZ, ALX ... */
    @Column(name = "code", nullable = false, length = 10)
    private String code;

    @Column(name = "name_ar", nullable = false, length = 100)
    private String nameAr;

    @Column(name = "name_en", nullable = false, length = 100)
    private String nameEn;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "display_order", nullable = false)
    private short displayOrder;

    public String nameFor(String locale) {
        return "en".equalsIgnoreCase(locale) ? nameEn : nameAr;
    }
}
