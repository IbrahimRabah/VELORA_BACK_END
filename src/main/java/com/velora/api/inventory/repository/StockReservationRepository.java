package com.velora.api.inventory.repository;

import com.velora.api.inventory.domain.ReservationStatus;
import com.velora.api.inventory.domain.StockReservation;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    List<StockReservation> findByCartIdAndStatus(Long cartId, ReservationStatus status);

    List<StockReservation> findByOrderIdAndStatus(Long orderId, ReservationStatus status);

    /**
     * Holds that timed out. Fetched in pages so a long outage does not produce one
     * enormous transaction on recovery.
     */
    @Query("""
            select r from StockReservation r
            where r.status = com.velora.api.inventory.domain.ReservationStatus.HELD
              and r.expiresAt < :now
            order by r.expiresAt asc
            """)
    List<StockReservation> findExpiredHolds(@Param("now") OffsetDateTime now,
                                            org.springframework.data.domain.Pageable pageable);

    /**
     * Operational signal, not a feature: a rising number here means checkouts are
     * being abandoned or a payment integration is stalling.
     */
    @Query("""
            select count(r) from StockReservation r
            where r.status = com.velora.api.inventory.domain.ReservationStatus.HELD
              and r.expiresAt < :now
            """)
    long countExpiredHolds(@Param("now") OffsetDateTime now);
}
