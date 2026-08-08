package com.velora.api.shipping.repository;

import com.velora.api.shipping.domain.ShippingZone;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShippingZoneRepository extends JpaRepository<ShippingZone, Long> {

    List<ShippingZone> findByActiveTrueOrderByIdAsc();

    Optional<ShippingZone> findByCode(String code);
}
