package com.velora.api.shipping.domain;

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
 * What a zone costs to ship to.
 *
 * <p>Currently flat: 70 EGP for Cairo and Lower Egypt, 100 for Upper Egypt. The
 * weight and free-threshold fields are configured but inactive — turning either on
 * is a data change, not a code change.
 */
@Entity
@Table(name = "shipping_rate")
@Getter
@Setter
@NoArgsConstructor
public class ShippingRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id", nullable = false)
    private ShippingZone zone;

    @Column(name = "base_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal baseCost;

    /** Weight included in {@code baseCost}. Null means weight is ignored. */
    @Column(name = "max_weight_grams")
    private Integer maxWeightGrams;

    @Column(name = "cost_per_extra_kg", nullable = false, precision = 19, scale = 4)
    private BigDecimal costPerExtraKg = BigDecimal.ZERO;

    /** Order value above which shipping is free. Null means never free. */
    @Column(name = "free_shipping_over", precision = 19, scale = 4)
    private BigDecimal freeShippingOver;

    /** Extra charge for collecting cash. Zero today. */
    @Column(name = "cod_fee", nullable = false, precision = 19, scale = 4)
    private BigDecimal codFee = BigDecimal.ZERO;

    @Column(name = "delivery_days_min", nullable = false)
    private short deliveryDaysMin = 2;

    @Column(name = "delivery_days_max", nullable = false)
    private short deliveryDaysMax = 5;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

    public boolean hasFreeThreshold() {
        return freeShippingOver != null;
    }

    public boolean isWeightBased() {
        return maxWeightGrams != null
                && costPerExtraKg.compareTo(BigDecimal.ZERO) > 0;
    }
}
