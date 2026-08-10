package com.velora.api.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Who changed what, when, and from what to what. Append-only.
 *
 * <p>This table answers questions that come up long after the fact and have no other
 * source: why a price is what it is, where six units went, who cancelled an invoice.
 * The domain tables hold the current value; this holds the history of decisions.
 *
 * <p>Deliberately NOT a foreign key to anything. Audit rows outlive the things they
 * describe — a product can be archived and a staff account deleted, and the record of
 * what they did must survive both.
 */
@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 40)
    private AuditAction action;

    /** PRODUCT_VARIANT, INVENTORY, INVOICE ... */
    @Column(name = "entity_type", nullable = false, length = 40)
    private String entityType;

    @Column(name = "entity_id", length = 60)
    private String entityId;

    /** A human label, so the log reads without joining anything. */
    @Column(name = "entity_label", length = 200)
    private String entityLabel;

    @Column(name = "old_value", length = 500)
    private String oldValue;

    @Column(name = "new_value", length = 500)
    private String newValue;

    /** Required for anything a person will need explained later. */
    @Column(name = "reason", length = 500)
    private String reason;

    /** Null when the change was automatic rather than made by a person. */
    @Column(name = "actor_id")
    private Long actorId;

    /** Copied, because the account may be renamed or removed later. */
    @Column(name = "actor_name", length = 150)
    private String actorName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);
}
