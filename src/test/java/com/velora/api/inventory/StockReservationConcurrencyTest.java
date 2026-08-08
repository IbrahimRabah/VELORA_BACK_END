package com.velora.api.inventory;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.velora.api.inventory.repository.StockReservationRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The test the whole inventory design exists for.
 *
 * <p>Overselling never shows up in development, because one person clicking one
 * button is not a race. It shows up on the first busy day, and the symptom is a
 * phone call apologising to a customer who already paid.
 *
 * <p>These tests run against the real database on purpose. The guarantee comes from
 * the database's row locking and the {@code WHERE} clause inside the UPDATE — mock
 * it out and you are testing nothing.
 */
@SpringBootTest
class StockReservationConcurrencyTest {

    private static final int THREADS = 20;

    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private StockReservationRepository reservationRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    private Long variantId;
    private Long productId;
    private Long categoryId;

    @BeforeEach
    void createTestStock() {
        String unique = UUID.randomUUID().toString().substring(0, 8);

        transactionTemplate.executeWithoutResult(status -> {
            Category category = new Category();
            category.setSlug("test-cat-" + unique);
            category.setActive(false);          // invisible to the storefront
            Category savedCategory = categoryRepository.save(category);
            categoryId = savedCategory.getId();

            Product product = new Product();
            product.setCategory(savedCategory);
            product.setSlug("test-product-" + unique);
            product.setStatus(ProductStatus.DRAFT);
            Product savedProduct = productRepository.save(product);
            productId = savedProduct.getId();

            ProductVariant variant = new ProductVariant();
            variant.setProduct(savedProduct);
            variant.setSku("TEST-" + unique.toUpperCase());
            variant.setPrice(new BigDecimal("100.0000"));
            variant.setTaxRate(new BigDecimal("0.1400"));
            variant.setStatus(VariantStatus.ACTIVE);
            ProductVariant savedVariant = variantRepository.save(variant);
            variantId = savedVariant.getId();
        });
    }

    @AfterEach
    void removeTestStock() {
        transactionTemplate.executeWithoutResult(status -> {
            reservationRepository.deleteAll(
                    reservationRepository.findAll().stream()
                            .filter(r -> r.getVariant().getId().equals(variantId))
                            .toList());
            inventoryRepository.findByVariantId(variantId)
                    .ifPresent(inventoryRepository::delete);
            variantRepository.findById(variantId).ifPresent(variantRepository::delete);
            productRepository.findById(productId).ifPresent(productRepository::delete);
            categoryRepository.findById(categoryId).ifPresent(categoryRepository::delete);
        });
    }

    @Test
    @DisplayName("20 customers race for the last unit — exactly one wins")
    void onlyOneReservationSucceedsForTheLastUnit() throws Exception {
        givenStock(1);

        Result result = runConcurrentReservations(THREADS, 1);

        assertThat(result.successes())
                .as("exactly one of %d parallel reservations may succeed", THREADS)
                .isEqualTo(1);
        assertThat(result.failures()).isEqualTo(THREADS - 1);

        Inventory after = reload();
        assertThat(after.getQtyReserved()).isEqualTo(1);
        assertThat(after.getAvailable()).isZero();
    }

    @Test
    @DisplayName("Five units, twenty racers — exactly five win and stock never goes negative")
    void reservationsStopExactlyAtAvailableStock() throws Exception {
        givenStock(5);

        Result result = runConcurrentReservations(THREADS, 1);

        assertThat(result.successes()).isEqualTo(5);
        assertThat(result.failures()).isEqualTo(THREADS - 5);

        Inventory after = reload();
        assertThat(after.getQtyReserved()).isEqualTo(5);
        assertThat(after.getAvailable()).isZero();
        assertThat(after.getQtyOnHand()).isEqualTo(5);   // holds do not consume stock
    }

    @Test
    @DisplayName("Ten units, requests of two — exactly five win")
    void handlesMultiUnitRequests() throws Exception {
        givenStock(10);

        Result result = runConcurrentReservations(THREADS, 2);

        assertThat(result.successes()).isEqualTo(5);

        Inventory after = reload();
        assertThat(after.getQtyReserved()).isEqualTo(10);
        assertThat(after.getAvailable()).isZero();
    }

    @Test
    @DisplayName("Available never goes negative, whatever the interleaving")
    void availableNeverGoesNegative() throws Exception {
        givenStock(3);

        runConcurrentReservations(THREADS, 2);

        Inventory after = reload();
        assertThat(after.getAvailable())
                .as("a negative available quantity means the guard failed")
                .isGreaterThanOrEqualTo(0);
        assertThat(after.getQtyReserved()).isLessThanOrEqualTo(after.getQtyOnHand());
    }

    @Test
    @DisplayName("Releasing returns the units to available")
    void releaseRestoresAvailability() {
        givenStock(2);

        transactionTemplate.executeWithoutResult(s ->
                assertThat(inventoryRepository.tryReserve(variantId, 2)).isEqualTo(1));
        assertThat(reload().getAvailable()).isZero();

        transactionTemplate.executeWithoutResult(s ->
                inventoryRepository.releaseReservation(variantId, 2));

        Inventory after = reload();
        assertThat(after.getAvailable()).isEqualTo(2);
        assertThat(after.getQtyReserved()).isZero();
    }

    @Test
    @DisplayName("Committing a hold reduces on hand, not available")
    void commitReducesOnHand() {
        givenStock(3);

        transactionTemplate.executeWithoutResult(s ->
                inventoryRepository.tryReserve(variantId, 2));
        transactionTemplate.executeWithoutResult(s ->
                assertThat(inventoryRepository.commitReservation(variantId, 2)).isEqualTo(1));

        Inventory after = reload();
        assertThat(after.getQtyOnHand()).isEqualTo(1);
        assertThat(after.getQtyReserved()).isZero();
        // Available was already 1 before the commit — shipping held units changes nothing.
        assertThat(after.getAvailable()).isEqualTo(1);
    }

    @Test
    @DisplayName("A double release cannot push reserved below zero")
    void doubleReleaseIsSafe() {
        givenStock(1);

        transactionTemplate.executeWithoutResult(s ->
                inventoryRepository.tryReserve(variantId, 1));
        transactionTemplate.executeWithoutResult(s ->
                inventoryRepository.releaseReservation(variantId, 1));
        transactionTemplate.executeWithoutResult(s ->
                inventoryRepository.releaseReservation(variantId, 1));

        Inventory after = reload();
        assertThat(after.getQtyReserved()).isZero();
        assertThat(after.getAvailable()).isEqualTo(1);
    }

    // ------------------------------------------------------------------ helpers

    private void givenStock(int quantity) {
        transactionTemplate.executeWithoutResult(status -> {
            ProductVariant variant = variantRepository.findById(variantId).orElseThrow();
            Inventory inventory = inventoryRepository.findByVariantId(variantId)
                    .orElseGet(() -> {
                        Inventory created = new Inventory();
                        created.setVariant(variant);
                        return created;
                    });
            inventory.setQtyOnHand(quantity);
            inventory.setQtyReserved(0);
            inventoryRepository.save(inventory);
        });
    }

    /**
     * Fires N reservations at the same instant.
     *
     * <p>The latch matters: without it the threads start in sequence and the test
     * passes even with a broken read-then-write implementation. They all have to be
     * inside the critical section together for the race to be real.
     */
    private Result runConcurrentReservations(int threads, int quantityEach) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();

        List<Callable<Void>> tasks = new java.util.ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(() -> {
                startGate.await(5, TimeUnit.SECONDS);
                try {
                    // Each attempt gets its own transaction, exactly like a real request.
                    Integer affected = transactionTemplate.execute(status ->
                            inventoryRepository.tryReserve(variantId, quantityEach));

                    if (affected != null && affected > 0) {
                        successes.incrementAndGet();
                    } else {
                        failures.incrementAndGet();
                    }
                } catch (Exception ex) {
                    // A deadlock or lock timeout is a refusal, not a sale.
                    failures.incrementAndGet();
                }
                return null;
            });
        }

        List<Future<Void>> futures = new java.util.ArrayList<>();
        for (Callable<Void> task : tasks) {
            futures.add(pool.submit(task));
        }

        startGate.countDown();
        for (Future<Void> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        return new Result(successes.get(), failures.get());
    }

    private Inventory reload() {
        return transactionTemplate.execute(status ->
                inventoryRepository.findByVariantId(variantId).orElseThrow());
    }

    private record Result(int successes, int failures) {
    }
}
