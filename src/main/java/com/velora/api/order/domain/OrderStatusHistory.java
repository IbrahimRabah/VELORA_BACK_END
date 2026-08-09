package com.velora.api.order.domain;

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
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Every status change, append-only.
 *
 * <p>The order row holds the current value; this table holds the truth. "When did
 * this ship?" and "who cancelled it?" are questions that come up constantly, and a
 * single mutable status column cannot answer either.
 */
@Entity
@Table(name = "order_status_history")
@Getter
@Setter
@NoArgsConstructor
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder order;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_kind", nullable = false, length = 20)
    private StatusKind statusKind;

    @Column(name = "from_status", length = 30)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 30)
    private String toStatus;

    @Column(name = "note", length = 500)
    private String note;

    /** Null when the change was automatic rather than made by a person. */
    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

    public static OrderStatusHistory of(CustomerOrder order, StatusKind kind,
                                        String from, String to, String note, Long actorId) {
        OrderStatusHistory entry = new OrderStatusHistory();
        entry.setOrder(order);
        entry.setStatusKind(kind);
        entry.setFromStatus(from);
        entry.setToStatus(to);
        entry.setNote(note);
        entry.setActorId(actorId);
        return entry;
    }
}
