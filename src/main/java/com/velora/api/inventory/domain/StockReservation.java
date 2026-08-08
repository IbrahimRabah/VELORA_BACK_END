package com.velora.api.inventory.domain;

import com.velora.api.catalog.domain.ProductVariant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A temporary hold on stock while a customer completes checkout.
 *
 * <p>The hold exists because the gap between "I clicked pay" and "the order is
 * created" is not instant. Without it, two customers can both pass the availability
 * check on the last unit.
 *
 * <p>Expiry is what stops abandoned checkouts from freezing inventory forever: a
 * scheduled job releases anything past {@code expiresAt}.
 */
@Entity
@Table(name = "stock_reservation")
@Getter
@Setter
@NoArgsConstructor
public class StockReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    /** Set once the order exists. */
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "cart_id")
    private Long cartId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.HELD;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

    public boolean isExpired() {
        return expiresAt.isBefore(OffsetDateTime.now(ZoneOffset.UTC));
    }

    public boolean isHeld() {
        return status == ReservationStatus.HELD;
    }
}
