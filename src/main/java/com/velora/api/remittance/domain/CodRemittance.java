package com.velora.api.remittance.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A batch of cash handed over by the courier.
 *
 * <p>This is where "delivered" becomes "paid". Without it, a cash-on-delivery store
 * has no way to know which money has actually arrived — the order says DELIVERED, the
 * cash is in a driver's bag, and a revenue report that treats the two as the same
 * overstates the bank balance by however many parcels are in transit.
 *
 * <p>Recording the batch is also what makes a shortfall visible. Nine thousand five
 * hundred against an expected ten thousand is a question, and the question has to be
 * asked while the courier still remembers the week.
 */
@Entity
@Table(name = "cod_remittance")
@Getter
@Setter
@NoArgsConstructor
public class CodRemittance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** REM-2026-0001 — what you quote back to the courier. */
    @Column(name = "reference", nullable = false, length = 40)
    private String reference;

    @Column(name = "courier_name", nullable = false, length = 150)
    private String courierName;

    /** The courier's own reference, so both sides can find the same batch. */
    @Column(name = "courier_reference", length = 100)
    private String courierReference;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RemittanceStatus status = RemittanceStatus.SETTLED;

    /** Sum of the included orders' totals — what should have arrived. */
    @Column(name = "expected_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal expectedAmount;

    /** What actually arrived. */
    @Column(name = "received_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal receivedAmount;

    /** received − expected. Negative is a shortfall. */
    @Column(name = "difference", nullable = false, precision = 19, scale = 4)
    private BigDecimal difference = BigDecimal.ZERO;

    @Column(name = "order_count", nullable = false)
    private int orderCount;

    /** Required when the amounts disagree. */
    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "recorded_by")
    private Long recordedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @OneToMany(mappedBy = "remittance", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CodRemittanceItem> items = new ArrayList<>();

    public boolean isShort() {
        return difference.compareTo(BigDecimal.ZERO) < 0;
    }

    public void addItem(CodRemittanceItem item) {
        item.setRemittance(this);
        items.add(item);
    }
}
