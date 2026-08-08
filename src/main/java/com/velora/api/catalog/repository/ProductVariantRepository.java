package com.velora.api.catalog.repository;

import com.velora.api.catalog.domain.ProductVariant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    Optional<ProductVariant> findBySku(String sku);

    Optional<ProductVariant> findByBarcode(String barcode);

    /**
     * All variants of a product with their defining attribute values, so the front
     * end can build the colour/size selectors without another call per variant.
     */
    @EntityGraph(attributePaths = {"attributeValues", "attributeValues.attribute",
            "attributeValues.attributeValue", "attributeValues.attributeValue.translations"})
    List<ProductVariant> findByProductIdAndArchivedAtIsNullOrderByPositionAsc(Long productId);

    boolean existsBySku(String sku);
}
