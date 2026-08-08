package com.velora.api.shipping.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Which zone a governorate belongs to.
 *
 * <p>The governorate id is the primary key, so a governorate can belong to exactly
 * one zone. That makes the rate lookup deterministic — there is never a question of
 * which of two matching rates applies.
 */
@Entity
@Table(name = "shipping_zone_governorate")
@Getter
@Setter
@NoArgsConstructor
public class ShippingZoneGovernorate {

    @Id
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "governorate_id", nullable = false)
    private Governorate governorate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id", nullable = false)
    private ShippingZone zone;
}
