package com.velora.api.inventory.repository;

import com.velora.api.inventory.domain.Inventory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByVariantId(Long variantId);

    List<Inventory> findByVariantIdIn(List<Long> variantIds);

    /**
     * THE concurrency guard. Reserves stock atomically and returns the number of
     * rows changed: 0 means another checkout took the units between your read and
     * your write, and the checkout must fail cleanly.
     *
     * <p>Never replace this with read-then-write. Under load, two customers buy the
     * last watch within the same millisecond and both succeed — and one of them
     * gets an apology instead of a product.
     */
    @Modifying
    @Query(value = """
            UPDATE inventory
            SET qty_reserved = qty_reserved + :qty, version = version + 1
            WHERE variant_id = :variantId
              AND (qty_on_hand - qty_reserved) >= :qty
            """, nativeQuery = true)
    int tryReserve(@Param("variantId") Long variantId, @Param("qty") int qty);

    @Modifying
    @Query(value = """
            UPDATE inventory
            SET qty_reserved = CASE WHEN qty_reserved >= :qty THEN qty_reserved - :qty ELSE 0 END,
                version = version + 1
            WHERE variant_id = :variantId
            """, nativeQuery = true)
    int releaseReservation(@Param("variantId") Long variantId, @Param("qty") int qty);

    /** Low-stock report for the admin dashboard. */
    @Query("""
            select i from Inventory i
            where (i.qtyOnHand - i.qtyReserved) <= i.minStockLevel
            order by (i.qtyOnHand - i.qtyReserved) asc
            """)
    List<Inventory> findLowStock();
}
