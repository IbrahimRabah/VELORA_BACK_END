package com.velora.api.remittance.repository;

import com.velora.api.remittance.domain.CodRemittance;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CodRemittanceRepository extends JpaRepository<CodRemittance, Long> {

    Optional<CodRemittance> findByReference(String reference);

    boolean existsByReference(String reference);

    @EntityGraph(attributePaths = {"items", "items.order"})
    Optional<CodRemittance> findWithItemsById(Long id);

    Page<CodRemittance> findAllByOrderBySettlementDateDesc(Pageable pageable);

    /** Highest sequence used this year, so a new reference never collides. */
    @Query(value = """
            SELECT COALESCE(MAX(CAST(RIGHT(reference, 4) AS INT)), 0)
            FROM cod_remittance
            WHERE reference LIKE :prefix + '%'
            """, nativeQuery = true)
    int highestSequenceFor(@Param("prefix") String prefix);

    @Query("""
            select coalesce(sum(r.difference), 0) from CodRemittance r
            where r.settlementDate >= :from
              and r.status <> com.velora.api.remittance.domain.RemittanceStatus.CANCELLED
            """)
    java.math.BigDecimal totalDifferenceSince(@Param("from") LocalDate from);
}
