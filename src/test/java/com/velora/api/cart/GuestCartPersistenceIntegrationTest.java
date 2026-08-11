package com.velora.api.cart;

import static org.assertj.core.api.Assertions.assertThat;

import com.velora.api.cart.dto.AddToCartRequest;
import com.velora.api.cart.dto.CartResponse;
import com.velora.api.cart.security.GuestTokenService;
import com.velora.api.cart.service.CartService;
import com.velora.api.catalog.domain.Category;
import com.velora.api.catalog.domain.Product;
import com.velora.api.catalog.domain.ProductStatus;
import com.velora.api.catalog.domain.ProductVariant;
import com.velora.api.catalog.domain.VariantStatus;
import com.velora.api.catalog.repository.CategoryRepository;
import com.velora.api.catalog.repository.ProductRepository;
import com.velora.api.catalog.repository.ProductVariantRepository;
import com.velora.api.inventory.domain.Inventory;
import com.velora.api.inventory.repository.InventoryRepository;
import java.math.BigDecimal;
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
 * A signed guest token must actually survive a cart save, not just verify.
 *
 * <p>This is exactly the gap that let the signing change ship with a truncation
 * bug: {@code GuestTokenServiceTest} proved the signature scheme itself was
 * correct, but nothing exercised INSERT-ing a signed token into
 * {@code cart.guest_token} — which was still {@code NVARCHAR(36)} against a
 * ~80-character signed token, so every guest add-to-cart failed with "String or
 * binary data would be truncated" (a 500, not a clean business error).
 *
 * <p>Runs against the real database on purpose, for the same reason
 * {@code PurchaseJourneyIntegrationTest} does — a column-width mismatch has no seam
 * for a mock to catch; only an actual INSERT does.
 */
@SpringBootTest
class GuestCartPersistenceIntegrationTest {

    private static final int OPENING_STOCK = 5;
    private static final BigDecimal UNIT_PRICE = new BigDecimal("1500.0000");

    @Autowired private CartService cartService;
    @Autowired private GuestTokenService guestTokenService;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private JdbcTemplate jdbc;

    private Long categoryId;
    private Long productId;
    private Long variantId;
    private Long cartId;

    @BeforeEach
    void createSellableProduct() {
        String unique = UUID.randomUUID().toString().substring(0, 8);

        transactionTemplate.executeWithoutResult(status -> {
            Category category = new Category();
            category.setSlug("guest-cart-cat-" + unique);
            category.setActive(false);
            categoryId = categoryRepository.save(category).getId();

            Product product = new Product();
            product.setCategory(categoryRepository.findById(categoryId).orElseThrow());
            product.setSlug("guest-cart-product-" + unique);
            product.setStatus(ProductStatus.ACTIVE);
            productId = productRepository.save(product).getId();

            ProductVariant variant = new ProductVariant();
            variant.setProduct(productRepository.findById(productId).orElseThrow());
            variant.setSku("GUEST-CART-" + unique.toUpperCase());
            variant.setPrice(UNIT_PRICE);
            variant.setTaxRate(new BigDecimal("0.1400"));
            variant.setWeightGrams(150);
            variant.setStatus(VariantStatus.ACTIVE);
            variantId = variantRepository.save(variant).getId();

            Inventory inventory = new Inventory();
            inventory.setVariant(variantRepository.findById(variantId).orElseThrow());
            inventory.setQtyOnHand(OPENING_STOCK);
            inventory.setQtyReserved(0);
            inventory.setMinStockLevel(1);
            inventoryRepository.save(inventory);
        });
    }

    @AfterEach
    void removeTestData() {
        if (cartId != null) {
            jdbc.update("DELETE FROM cart_item WHERE cart_id = ?", cartId);
            jdbc.update("DELETE FROM cart WHERE id = ?", cartId);
        }
        jdbc.update("DELETE FROM inventory WHERE variant_id = ?", variantId);
        jdbc.update("DELETE FROM product_variant WHERE id = ?", variantId);
        jdbc.update("DELETE FROM product WHERE id = ?", productId);
        jdbc.update("DELETE FROM category WHERE id = ?", categoryId);
    }

    @Test
    @DisplayName("A signed guest token survives add-to-cart, save and re-read")
    void signedGuestTokenSurvivesCartPersistence() {
        String guestToken = guestTokenService.generate();
        // Longer than the old NVARCHAR(36) — this is the shape that used to truncate.
        assertThat(guestToken.length()).isGreaterThan(36);

        CartResponse afterAdd = cartService.addItem(
                null, guestToken, new AddToCartRequest(variantId, 2), "ar");
        cartId = afterAdd.cartId();

        assertThat(afterAdd.items()).hasSize(1);
        assertThat(afterAdd.items().get(0).quantity()).isEqualTo(2);

        // A fresh lookup by the same token — proves the token round-tripped through
        // the guest_token column exactly, not just that the in-memory response
        // returned from addItem() looked right.
        CartResponse reloaded = cartService.getCart(null, guestToken, "ar");

        assertThat(reloaded.cartId()).isEqualTo(cartId);
        assertThat(reloaded.items()).hasSize(1);
        assertThat(reloaded.items().get(0).variantId()).isEqualTo(variantId);
        assertThat(reloaded.items().get(0).quantity()).isEqualTo(2);
    }
}
