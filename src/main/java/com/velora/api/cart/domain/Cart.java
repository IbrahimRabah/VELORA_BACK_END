package com.velora.api.cart.domain;

import com.velora.api.identity.domain.AppUser;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A cart is a PROPOSAL, never a promise.
 *
 * <p>Nothing here reserves stock and nothing here fixes a price. Both are
 * re-evaluated against the database at checkout, and the customer is shown any
 * difference before the order is created. A cart that guaranteed its prices would
 * either need to hold stock indefinitely or lie to the customer.
 *
 * <p>Owned by a user OR by an anonymous {@code guestToken} — the database enforces
 * that at least one is present.
 */
@Entity
@Table(name = "cart")
@Getter
@Setter
@NoArgsConstructor
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    /** Random UUID stored in a browser cookie before the customer signs in. */
    @Column(name = "guest_token", length = 64)
    private String guestToken;

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CartStatus status = CartStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now(ZoneOffset.UTC);

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("addedAt ASC, id ASC")
    private List<CartItem> items = new ArrayList<>();

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int totalQuantity() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    public void touch() {
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void addItem(CartItem item) {
        item.setCart(this);
        items.add(item);
        touch();
    }

    public void removeItem(CartItem item) {
        items.remove(item);
        touch();
    }
}
