package com.velora.api.inventory.repository;

import com.velora.api.inventory.domain.ReservationStatus;
import com.velora.api.inventory.domain.StockReservation;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    List<StockReservation> findByCartIdAndStatus(Long cartId, ReservationStatus status);

    List<StockReservation> findByOrderIdAndStatus(Long orderId, ReservationStatus status);

    /**
     * Holds that timed out and may be returned to available stock.
     *
     * <p>{@code orderId is null} is the condition that matters. The TTL exists to free
     * stock from ABANDONED CHECKOUTS — a cart that was filled and never submitted.
     * Once an order exists the sale has happened, and its hold must survive until the
     * parcel ships, however many days that takes.
     *
     * <p>Without this filter a slow dispatch quietly returns sold stock to the shelf.
     * Nothing looks wrong at the time: the order ships, the numbers just never move.
     * The shortage surfaces later, when a customer buys something that is already
     * in a box on its way to someone else.
     */
    @Query("""
            select r from StockReservation r
            where r.status = com.velora.api.inventory.domain.ReservationStatus.HELD
              and r.expiresAt < :now
              and r.orderId is null
            order by r.expiresAt asc
            """)
    List<StockReservation> findExpiredHolds(@Param("now") OffsetDateTime now,
                                            Pageable pageable);

    /**
     * Operational signal, not a feature: a rising number here means checkouts are
     * being abandoned, or a payment integration has stopped confirming.
     *
     * <p>Counts the same set {@link #findExpiredHolds} releases, so the two never
     * disagree about what is actually pending.
     */
    @Query("""
            select count(r) from StockReservation r
            where r.status = com.velora.api.inventory.domain.ReservationStatus.HELD
              and r.expiresAt < :now
              and r.orderId is null
            """)
    long countExpiredHolds(@Param("now") OffsetDateTime now);

    /**
     * Holds attached to an order that is long past shipping.
     *
     * <p>These are never expired automatically — an order-backed hold is a real
     * commitment. But one sitting here for days means the order is stuck somewhere in
     * fulfilment, and that is worth seeing.
     */
    @Query("""
            select r from StockReservation r
            where r.status = com.velora.api.inventory.domain.ReservationStatus.HELD
              and r.orderId is not null
              and r.createdAt < :cutoff
            order by r.createdAt asc
            """)
    List<StockReservation> findStaleOrderHolds(@Param("cutoff") OffsetDateTime cutoff);
}