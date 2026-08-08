package com.velora.api.cart.repository;

import com.velora.api.cart.domain.Cart;
import com.velora.api.cart.domain.CartStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * The whole cart in one round trip. With {@code open-in-view: false} a lazy
     * access after the service returns would throw, so the graph is fetched here.
     */
    @EntityGraph(attributePaths = {"items", "items.variant", "items.variant.product"})
    Optional<Cart> findByUserIdAndStatus(Long userId, CartStatus status);

    @EntityGraph(attributePaths = {"items", "items.variant", "items.variant.product"})
    Optional<Cart> findByGuestTokenAndStatus(String guestToken, CartStatus status);

    @EntityGraph(attributePaths = {"items", "items.variant", "items.variant.product"})
    Optional<Cart> findByIdAndStatus(Long id, CartStatus status);

    /** Abandoned-cart candidates. Recovery emails are P1; the query is ready. */
    @Query("""
            select c from Cart c
            where c.status = com.velora.api.cart.domain.CartStatus.ACTIVE
              and c.updatedAt < :cutoff
              and size(c.items) > 0
            """)
    List<Cart> findStaleCarts(@Param("cutoff") OffsetDateTime cutoff);
}
