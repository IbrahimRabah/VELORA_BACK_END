package com.velora.api.catalog.spec;

import com.velora.api.catalog.domain.Product;
import com.velora.api.catalog.domain.ProductStatus;
import com.velora.api.catalog.domain.ProductTranslation;
import com.velora.api.catalog.domain.ProductVariant;
import com.velora.api.catalog.domain.VariantAttributeValue;
import com.velora.api.catalog.domain.VariantStatus;
import com.velora.api.common.util.ArabicNormalizer;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.util.Collection;
import org.springframework.data.jpa.domain.Specification;

/**
 * Composable filters for the product catalog.
 *
 * <p>Why the Specification pattern and not repository methods: product search has
 * eight optional filters. As derived query methods that would be
 * {@code findByCategoryAndBrandInAndPriceBetweenAndStatus...} multiplied by every
 * combination — hundreds of methods, or hand-concatenated JPQL. Specifications
 * compose the same filters into one query at runtime, and a null filter simply
 * contributes nothing.
 *
 * <pre>
 * Specification&lt;Product&gt; spec = Specification
 *         .where(ProductSpecifications.isVisible())
 *         .and(ProductSpecifications.inCategory(filter.categoryId()))
 *         .and(ProductSpecifications.priceBetween(filter.minPrice(), filter.maxPrice()))
 *         .and(ProductSpecifications.hasAnyAttributeValue(filter.attributeValueIds()))
 *         .and(ProductSpecifications.inStock(filter.inStockOnly()))
 *         .and(ProductSpecifications.matches(filter.q(), locale));
 * </pre>
 */
public final class ProductSpecifications {

    private ProductSpecifications() {
        // utility class
    }

    /** Active and not archived. Apply to every public query. */
    public static Specification<Product> isVisible() {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), ProductStatus.ACTIVE),
                cb.isNull(root.get("archivedAt")));
    }

    /**
     * Matches the category itself and its direct children, so browsing "Watches"
     * shows everything in "Men Watches" too.
     */
    public static Specification<Product> inCategory(Long categoryId) {
        if (categoryId == null) {
            return alwaysTrue();
        }
        return (root, query, cb) -> {
            var category = root.join("category", JoinType.INNER);
            return cb.or(
                    cb.equal(category.get("id"), categoryId),
                    cb.equal(category.get("parent").get("id"), categoryId));
        };
    }

    public static Specification<Product> inBrands(Collection<Long> brandIds) {
        if (brandIds == null || brandIds.isEmpty()) {
            return alwaysTrue();
        }
        return (root, query, cb) -> root.join("brand", JoinType.INNER).get("id").in(brandIds);
    }

    public static Specification<Product> isFeatured(Boolean featured) {
        if (featured == null || !featured) {
            return alwaysTrue();
        }
        return (root, query, cb) -> cb.isTrue(root.get("featured"));
    }

    public static Specification<Product> isNewArrival(Boolean newArrival) {
        if (newArrival == null || !newArrival) {
            return alwaysTrue();
        }
        return (root, query, cb) -> cb.isTrue(root.get("newArrival"));
    }

    /**
     * Filters on the {@code minPrice} / {@code maxPrice} formula fields, so no join
     * or DISTINCT is needed and the page count stays correct.
     */
    public static Specification<Product> priceBetween(BigDecimal min, BigDecimal max) {
        if (min == null && max == null) {
            return alwaysTrue();
        }
        return (root, query, cb) -> {
            if (min != null && max != null) {
                // A product matches if any of its variants falls in the range.
                return cb.and(
                        cb.lessThanOrEqualTo(root.get("minPrice"), max),
                        cb.greaterThanOrEqualTo(root.get("maxPrice"), min));
            }
            if (min != null) {
                return cb.greaterThanOrEqualTo(root.get("maxPrice"), min);
            }
            return cb.lessThanOrEqualTo(root.get("minPrice"), max);
        };
    }

    /**
     * Products having at least one variant carrying any of the given attribute
     * values — colour = gold OR silver.
     *
     * <p>Uses an EXISTS subquery rather than a join, which keeps the row count
     * unchanged and avoids the DISTINCT that would otherwise break pagination.
     */
    public static Specification<Product> hasAnyAttributeValue(Collection<Long> attributeValueIds) {
        if (attributeValueIds == null || attributeValueIds.isEmpty()) {
            return alwaysTrue();
        }
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            var variant = sub.from(ProductVariant.class);
            Join<ProductVariant, VariantAttributeValue> vav = variant.join("attributeValues");

            sub.select(variant.get("id"))
                    .where(cb.and(
                            cb.equal(variant.get("product").get("id"), root.get("id")),
                            cb.equal(variant.get("status"), VariantStatus.ACTIVE),
                            cb.isNull(variant.get("archivedAt")),
                            vav.get("attributeValue").get("id").in(attributeValueIds)));

            return cb.exists(sub);
        };
    }

    /** Only products with at least one unit actually available. */
    public static Specification<Product> inStock(Boolean inStockOnly) {
        if (inStockOnly == null || !inStockOnly) {
            return alwaysTrue();
        }
        return (root, query, cb) -> cb.greaterThan(root.get("availableQty"), 0);
    }

    /**
     * Arabic-aware search.
     *
     * <p>The query is normalized with the SAME function used when writing
     * {@code search_text}. Without that, a customer typing {@code ساعه ذهبى} would
     * not match a product stored as {@code ساعة ذهبي} — which is most customers.
     */
    public static Specification<Product> matches(String rawQuery, String locale) {
        String normalized = ArabicNormalizer.normalize(rawQuery);
        if (normalized == null) {
            return alwaysTrue();
        }
        String pattern = "%" + normalized.replace(" ", "%") + "%";

        return (root, query, cb) -> {
            Join<Product, ProductTranslation> tr = root.join("translations", JoinType.LEFT);
            return cb.and(
                    cb.equal(tr.get("key").get("locale"), locale),
                    cb.or(
                            cb.like(cb.lower(tr.get("searchText")), pattern),
                            cb.like(cb.lower(tr.get("name")), pattern),
                            cb.like(cb.lower(root.get("slug")), pattern)));
        };
    }
    /**
     * Spring Data JPA 4.x rejects a null argument to and(), unlike earlier versions.
     * An inactive filter must therefore contribute an always-true predicate rather
     * than null, so composition still works when nothing is filtered.
     */
    private static Specification<Product> alwaysTrue() {
        return (root, query, cb) -> cb.conjunction();
    }
}
