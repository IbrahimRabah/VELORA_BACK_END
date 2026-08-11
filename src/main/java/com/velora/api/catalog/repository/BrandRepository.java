package com.velora.api.catalog.repository;

import com.velora.api.catalog.domain.Brand;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    List<Brand> findByActiveTrueOrderByNameArAsc();

    /** Admin variant of the storefront list: includes inactive brands too. */
    List<Brand> findAllByOrderByNameArAsc();

    Optional<Brand> findBySlugAndActiveTrue(String slug);

    /** Checked before insert so the user gets BRAND_SLUG_EXISTS, not a raw 500. */
    boolean existsBySlug(String slug);
}
