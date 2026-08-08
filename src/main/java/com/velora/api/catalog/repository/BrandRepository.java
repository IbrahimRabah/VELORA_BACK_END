package com.velora.api.catalog.repository;

import com.velora.api.catalog.domain.Brand;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    List<Brand> findByActiveTrueOrderByNameArAsc();

    Optional<Brand> findBySlugAndActiveTrue(String slug);
}
