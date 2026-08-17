package com.velora.api.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.velora.api.catalog.domain.Category;
import com.velora.api.catalog.domain.CategoryTranslation;
import com.velora.api.catalog.domain.Product;
import com.velora.api.catalog.domain.ProductImage;
import com.velora.api.catalog.domain.ProductStatus;
import com.velora.api.catalog.domain.ProductTranslation;
import com.velora.api.catalog.domain.ProductVariant;
import com.velora.api.catalog.domain.VariantStatus;
import com.velora.api.catalog.dto.ImageResponse;
import com.velora.api.catalog.dto.ProductDetailResponse;
import com.velora.api.catalog.dto.VariantResponse;
import com.velora.api.catalog.repository.CategoryRepository;
import com.velora.api.catalog.repository.ProductRepository;
import com.velora.api.catalog.service.ProductQueryService;
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
 * {@code ProductRepository.findBySlugAndArchivedAtIsNull} fetch-joins {@code images}
 * alongside {@code translations} and {@code category.translations} in one query.
 * Fetch-joining three collections together is a cartesian product at the row level: a
 * product translated into 2 locales, whose category is also translated into 2 locales,
 * makes Hibernate hydrate every {@code ProductImage} row 2 x 2 = 4 times — the exact
 * "same id four times" symptom, and the same root cause already known from
 * {@code AttributeRepository} and {@code ProductVariantRepository} (see
 * {@link VariantAttributeDuplicationIntegrationTest}).
 *
 * <p>Runs against the real database: the duplication only exists once Hibernate
 * actually executes the fetch-joined SQL, which a mocked repository would not surface.
 */
@SpringBootTest
class ProductImageDuplicationIntegrationTest {

    @Autowired private ProductQueryService productQueryService;

    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;

    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private JdbcTemplate jdbc;

    private Long categoryId;
    private Long productId;
    private Long variantAId;
    private Long variantBId;
    private Long sharedImageId;
    private Long variantAImageId;
    private Long variantBImageId;
    private String productSlug;

    @BeforeEach
    void createProductWithMultipleImagesAndVariants() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        productSlug = "dup-img-test-" + unique;

        transactionTemplate.executeWithoutResult(status -> {
            // Category translated into ar + en — one half of the cartesian join.
            Category category = new Category();
            category.setSlug("dup-img-cat-" + unique);
            category.setActive(true);
            putCategoryTranslation(category, "ar", "فئة تجريبية");
            putCategoryTranslation(category, "en", "Test Category");
            category = categoryRepository.save(category);
            categoryId = category.getId();

            // Product translated into ar + en — the other half of the cartesian join.
            Product product = new Product();
            product.setCategory(category);
            product.setSlug(productSlug);
            product.setStatus(ProductStatus.ACTIVE);
            putProductTranslation(product, "ar", "منتج تجريبي");
            putProductTranslation(product, "en", "Test Product");

            ProductVariant variantA = newVariant(product, "A-" + unique, (short) 1);
            ProductVariant variantB = newVariant(product, "B-" + unique, (short) 2);
            product.getVariants().add(variantA);
            product.getVariants().add(variantB);

            // Four images: two shared across all variants, one specific to each variant —
            // this is the shape reported: 4 real images, each seen 4 times over the wire.
            ProductImage shared1 = newImage(product, null, "img-shared-1.jpg", true, (short) 1);
            ProductImage shared2 = newImage(product, null, "img-shared-2.jpg", false, (short) 2);
            ProductImage forA = newImage(product, variantA, "img-variant-a.jpg", false, (short) 3);
            ProductImage forB = newImage(product, variantB, "img-variant-b.jpg", false, (short) 4);
            product.getImages().add(shared1);
            product.getImages().add(shared2);
            product.getImages().add(forA);
            product.getImages().add(forB);

            product = productRepository.save(product);

            productId = product.getId();
            variantAId = variantA.getId();
            variantBId = variantB.getId();
            sharedImageId = shared1.getId();
            variantAImageId = forA.getId();
            variantBImageId = forB.getId();
        });
    }

    @AfterEach
    void removeTestData() {
        jdbc.update("DELETE FROM product_image WHERE product_id = ?", productId);
        jdbc.update("DELETE FROM product_variant WHERE product_id = ?", productId);
        jdbc.update("DELETE FROM product_translation WHERE product_id = ?", productId);
        jdbc.update("DELETE FROM product WHERE id = ?", productId);
        jdbc.update("DELETE FROM category_translation WHERE category_id = ?", categoryId);
        jdbc.update("DELETE FROM category WHERE id = ?", categoryId);
    }

    @Test
    @DisplayName("Storefront product page returns each image exactly once, not once per image")
    void storefrontProductDetail_doesNotDuplicateImages() {
        ProductDetailResponse detail = productQueryService.findBySlug(productSlug, "ar");

        assertThat(detail.images()).hasSize(4);
        assertThat(detail.images().stream().map(ImageResponse::id).distinct().toList())
                .hasSize(4);

        assertThat(detail.variants()).hasSize(2);
    }

    @Test
    @DisplayName("Each variant's gallery contains only its own image, exactly once")
    void variantGallery_doesNotDuplicateItsOwnImage() {
        ProductDetailResponse detail = productQueryService.findBySlug(productSlug, "ar");

        VariantResponse variantA = detail.variants().stream()
                .filter(v -> v.id().equals(variantAId))
                .findFirst().orElseThrow();
        VariantResponse variantB = detail.variants().stream()
                .filter(v -> v.id().equals(variantBId))
                .findFirst().orElseThrow();

        assertThat(variantA.images()).hasSize(1);
        assertThat(variantA.images().get(0).id()).isEqualTo(variantAImageId);

        assertThat(variantB.images()).hasSize(1);
        assertThat(variantB.images().get(0).id()).isEqualTo(variantBImageId);
    }

    private ProductVariant newVariant(Product product, String skuSuffix, short position) {
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSku("DUPIMG-" + skuSuffix.toUpperCase());
        variant.setPrice(new BigDecimal("1500.0000"));
        variant.setTaxRate(new BigDecimal("0.1400"));
        variant.setWeightGrams(100);
        variant.setStatus(VariantStatus.ACTIVE);
        variant.setPosition(position);
        return variant;
    }

    private ProductImage newImage(Product product, ProductVariant variant, String key,
                                  boolean main, short displayOrder) {
        ProductImage image = new ProductImage();
        image.setProduct(product);
        image.setVariant(variant);
        image.setUrl("products/dup-test/" + key);
        image.setMain(main);
        image.setDisplayOrder(displayOrder);
        return image;
    }

    private void putCategoryTranslation(Category category, String locale, String name) {
        CategoryTranslation translation = new CategoryTranslation();
        translation.attachTo(category, locale);
        translation.setName(name);
        category.getTranslations().put(locale, translation);
    }

    private void putProductTranslation(Product product, String locale, String name) {
        ProductTranslation translation = new ProductTranslation();
        translation.attachTo(product, locale);
        translation.setName(name);
        product.getTranslations().put(locale, translation);
    }
}
