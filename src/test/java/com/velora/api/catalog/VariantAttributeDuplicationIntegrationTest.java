package com.velora.api.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.velora.api.catalog.domain.Attribute;
import com.velora.api.catalog.domain.AttributeValue;
import com.velora.api.catalog.domain.AttributeValueTranslation;
import com.velora.api.catalog.domain.Category;
import com.velora.api.catalog.domain.Product;
import com.velora.api.catalog.domain.ProductStatus;
import com.velora.api.catalog.domain.ProductVariant;
import com.velora.api.catalog.domain.VariantAttributeValue;
import com.velora.api.catalog.domain.VariantStatus;
import com.velora.api.catalog.dto.ProductDetailResponse;
import com.velora.api.catalog.dto.VariantResponse;
import com.velora.api.catalog.dto.admin.VariantAdminResponse;
import com.velora.api.catalog.repository.AttributeRepository;
import com.velora.api.catalog.repository.CategoryRepository;
import com.velora.api.catalog.repository.ProductRepository;
import com.velora.api.catalog.repository.ProductVariantRepository;
import com.velora.api.catalog.service.ProductQueryService;
import com.velora.api.catalog.service.admin.VariantAdminService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@code ProductVariantRepository.findByProductIdAndArchivedAtIsNullOrderByPositionAsc}
 * fetch-joins {@code attributeValues} alongside {@code attributeValue.translations} in
 * one query. Fetch-joining two bags together is a cartesian product at the row level:
 * an attribute value with two translations (ar + en) makes Hibernate hydrate the SAME
 * {@code VariantAttributeValue} row twice — the exact issue already known from
 * {@code AttributeRepository} (see {@code TaxonomyAdminServiceIntegrationTest}).
 *
 * <p>Every reader of that query result — admin variants list AND the storefront
 * product page — must collapse the duplicate back with {@code distinct()}, or the
 * customer sees "وردي هادئ / وردي هادئ" and {@code attributeValueIds: [56, 56]}.
 *
 * <p>Runs against the real database: the duplication only exists once Hibernate
 * actually executes the fetch-joined SQL, which a mocked repository would not surface.
 */
@SpringBootTest
class VariantAttributeDuplicationIntegrationTest {

    @Autowired private VariantAdminService variantAdminService;
    @Autowired private ProductQueryService productQueryService;

    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private AttributeRepository attributeRepository;
    @Autowired private ProductVariantRepository variantRepository;

    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private JdbcTemplate jdbc;

    private Long categoryId;
    private Long productId;
    private Long attributeId;
    private Long attributeValueId;
    private Long variantId;
    private String productSlug;

    @BeforeEach
    void createProductWithTranslatedVariantValue() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        productSlug = "dup-test-" + unique;

        transactionTemplate.executeWithoutResult(status -> {
            Category category = new Category();
            category.setSlug("dup-test-cat-" + unique);
            category.setActive(true);
            categoryId = categoryRepository.save(category).getId();

            Product product = new Product();
            product.setCategory(categoryRepository.findById(categoryId).orElseThrow());
            product.setSlug(productSlug);
            product.setStatus(ProductStatus.ACTIVE);
            productId = productRepository.save(product).getId();

            // Variant-defining attribute whose value has BOTH an Arabic and an
            // English translation — the condition that triggers the cartesian join.
            Attribute attribute = new Attribute();
            attribute.setCode("DUP_COLOR_" + unique.toUpperCase());
            attribute.setVariantDefining(true);
            attribute.setFilterable(true);
            attribute.setDisplayOrder((short) 1);

            AttributeValue value = new AttributeValue();
            value.setAttribute(attribute);
            value.setCode("ROSE_" + unique.toUpperCase());
            value.setDisplayOrder((short) 1);
            putValueTranslation(value, "ar", "وردي هادئ");
            putValueTranslation(value, "en", "Soft Rose");
            attribute.getValues().add(value);

            Attribute savedAttribute = attributeRepository.save(attribute);
            attributeId = savedAttribute.getId();
            attributeValueId = savedAttribute.getValues().get(0).getId();

            ProductVariant variant = new ProductVariant();
            variant.setProduct(productRepository.findById(productId).orElseThrow());
            variant.setSku("DUP-" + unique.toUpperCase());
            variant.setPrice(new BigDecimal("1500.0000"));
            variant.setTaxRate(new BigDecimal("0.1400"));
            variant.setWeightGrams(100);
            variant.setStatus(VariantStatus.ACTIVE);

            VariantAttributeValue vav = new VariantAttributeValue();
            vav.setKey(new VariantAttributeValue.Key());
            vav.setVariant(variant);
            vav.setAttribute(attributeRepository.findById(attributeId).orElseThrow());
            vav.setAttributeValue(attributeRepository.findById(attributeId).orElseThrow()
                    .getValues().get(0));
            variant.getAttributeValues().add(vav);

            variantId = variantRepository.save(variant).getId();
        });
    }

    @AfterEach
    void removeTestData() {
        jdbc.update("DELETE FROM variant_attribute_value WHERE variant_id = ?", variantId);
        jdbc.update("DELETE FROM product_variant WHERE id = ?", variantId);
        jdbc.update("DELETE FROM product_translation WHERE product_id = ?", productId);
        jdbc.update("DELETE FROM product WHERE id = ?", productId);
        jdbc.update("DELETE FROM category WHERE id = ?", categoryId);
        jdbc.update("DELETE FROM attribute_value_translation WHERE attribute_value_id = ?",
                attributeValueId);
        jdbc.update("DELETE FROM attribute_value WHERE id = ?", attributeValueId);
        jdbc.update("DELETE FROM attribute WHERE id = ?", attributeId);
    }

    @Test
    @DisplayName("Admin variant list does not duplicate attributeValueIds or the summary")
    void adminVariantList_doesNotDuplicateTranslatedValue() {
        List<VariantAdminResponse> variants = variantAdminService.listForProduct(productId);

        assertThat(variants).hasSize(1);
        VariantAdminResponse variant = variants.get(0);

        assertThat(variant.attributeValueIds()).containsExactly(attributeValueId);
        assertThat(variant.summary()).isEqualTo("وردي هادئ");
    }

    @Test
    @DisplayName("Storefront product page does not duplicate attributeValueIds or the summary")
    void storefrontProductDetail_doesNotDuplicateTranslatedValue() {
        ProductDetailResponse detail = productQueryService.findBySlug(productSlug, "ar");

        assertThat(detail.variants()).hasSize(1);
        VariantResponse variant = detail.variants().get(0);

        assertThat(variant.attributeValueIds()).containsExactly(attributeValueId);
        assertThat(variant.summary()).isEqualTo("وردي هادئ");

        assertThat(detail.variantOptions()).hasSize(1);
        assertThat(detail.variantOptions().get(0).values()).hasSize(1);
    }

    private void putValueTranslation(AttributeValue value, String locale, String name) {
        AttributeValueTranslation translation = new AttributeValueTranslation();
        translation.attachTo(value, locale);
        translation.setName(name);
        value.getTranslations().put(locale, translation);
    }
}
