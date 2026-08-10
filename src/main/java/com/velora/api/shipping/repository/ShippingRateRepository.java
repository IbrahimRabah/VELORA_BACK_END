package com.velora.api.shipping.repository;

import com.velora.api.shipping.domain.ShippingRate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;
public interface ShippingRateRepository extends JpaRepository<ShippingRate, Long> {

    /**
     * The rate for a governorate, in one query.
     *
     * <p>Because a governorate maps to exactly one zone, this can never return two
     * competing rates for the same destination.
     */
    @EntityGraph(attributePaths = {"zone"})
    @Query("""
            select r from ShippingRate r
            join r.zone z
            join ShippingZoneGovernorate zg on zg.zone.id = z.id
            where zg.governorate.id = :governorateId
              and r.active = true
              and z.active = true
            """)
    Optional<ShippingRate> findForGovernorate(@Param("governorateId") Long governorateId);

    List<ShippingRate> findByActiveTrueOrderByIdAsc();

    Optional<ShippingRate> findByZoneIdAndActiveTrue(Long zoneId);
}
