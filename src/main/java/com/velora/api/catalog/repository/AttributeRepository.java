package com.velora.api.catalog.repository;

import com.velora.api.catalog.domain.Attribute;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttributeRepository extends JpaRepository<Attribute, Long> {

    @EntityGraph(attributePaths = {"translations", "values", "values.translations"})
    List<Attribute> findByFilterableTrueOrderByDisplayOrderAsc();

    /**
     * Filter facets for a category: only the attribute values that actually occur on
     * products in that category. Showing a colour with zero results is a dead end
     * for the customer.
     */
    @Query("""
            select distinct a from Attribute a
            join a.values v
            join VariantAttributeValue vav on vav.attributeValue.id = v.id
            join vav.variant var
            join var.product p
            where a.filterable = true
              and p.status = com.velora.api.catalog.domain.ProductStatus.ACTIVE
              and p.archivedAt is null
              and (p.category.id = :categoryId or p.category.parent.id = :categoryId)
            order by a.displayOrder asc
            """)
    List<Attribute> findFacetsForCategory(@Param("categoryId") Long categoryId);
}
