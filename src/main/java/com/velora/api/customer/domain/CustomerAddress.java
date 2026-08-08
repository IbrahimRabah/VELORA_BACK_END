package com.velora.api.customer.domain;

import com.velora.api.identity.domain.AppUser;
import com.velora.api.shipping.domain.Governorate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A saved delivery address.
 *
 * <p>Shaped for Egypt, not for an international form. There is no postal code
 * because it is not reliably used and cannot route a parcel; the governorate does
 * that. {@code landmark} is a real field rather than an afterthought — couriers here
 * genuinely navigate by "next to Al-Nour mosque".
 *
 * <p>Two phone numbers because a failed delivery is usually an unanswered phone.
 *
 * <p>Note orders do NOT reference this row. They copy the address, because customers
 * edit and delete addresses and a delivered order must always show where it actually
 * went.
 */
@Entity
@Table(name = "customer_address")
@Getter
@Setter
@NoArgsConstructor
public class CustomerAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    /** HOME, WORK, OTHER — for the customer's own convenience. */
    @Column(name = "label", length = 30)
    private String label;

    /** May differ from the account holder: gifts and family deliveries are common. */
    @Column(name = "recipient_name", nullable = false, length = 150)
    private String recipientName;

    @Column(name = "phone_e164", nullable = false, length = 20)
    private String phoneE164;

    @Column(name = "alt_phone_e164", length = 20)
    private String altPhoneE164;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "governorate_id", nullable = false)
    private Governorate governorate;

    /**
     * Free text. Courier city lists differ per company, so this stays text until a
     * courier is chosen and its list can be adopted wholesale.
     */
    @Column(name = "city_id")
    private Long cityId;

    @Column(name = "area", length = 150)
    private String area;

    @Column(name = "street_address", nullable = false, length = 255)
    private String streetAddress;

    @Column(name = "building", length = 50)
    private String building;

    @Column(name = "floor", length = 20)
    private String floor;

    @Column(name = "apartment", length = 20)
    private String apartment;

    @Column(name = "landmark", length = 255)
    private String landmark;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "is_default", nullable = false)
    private boolean defaultAddress;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /** One line for courier labels and order snapshots. */
    public String formatted() {
        StringBuilder sb = new StringBuilder(streetAddress);
        if (building != null && !building.isBlank()) {
            sb.append(", building ").append(building);
        }
        if (floor != null && !floor.isBlank()) {
            sb.append(", floor ").append(floor);
        }
        if (apartment != null && !apartment.isBlank()) {
            sb.append(", apt ").append(apartment);
        }
        if (area != null && !area.isBlank()) {
            sb.append(", ").append(area);
        }
        if (landmark != null && !landmark.isBlank()) {
            sb.append(" (").append(landmark).append(')');
        }
        return sb.toString();
    }
}
