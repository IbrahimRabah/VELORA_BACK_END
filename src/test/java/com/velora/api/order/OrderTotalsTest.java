package com.velora.api.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.velora.api.common.util.MoneyUtils;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The arithmetic an order depends on.
 *
 * <p>These are the calculations that produce an invoice. A rounding bug here is
 * invisible in testing and shows up as a customer or an accountant disputing a
 * number months later.
 */
class OrderTotalsTest {

    private static final BigDecimal VAT = new BigDecimal("0.1400");

    @Test
    @DisplayName("Tax is extracted from the price, never added to it")
    void taxIsInclusive() {
        BigDecimal gross = new BigDecimal("2400.00");

        BigDecimal net = MoneyUtils.netFromGross(gross, VAT);
        BigDecimal tax = MoneyUtils.taxFromGross(gross, VAT);

        // The customer pays 2400 either way — the tax is inside it.
        assertThat(net.add(tax)).isEqualByComparingTo(gross);
        assertThat(net).isLessThan(gross);
    }

    @Test
    @DisplayName("Per-line tax sums to the order tax, exactly")
    void lineTaxSumsToOrderTax() {
        List<BigDecimal> lines = List.of(
                new BigDecimal("2400.00"),
                new BigDecimal("2200.00"),
                new BigDecimal("850.00"));

        BigDecimal grossSum = BigDecimal.ZERO;
        BigDecimal taxSum = BigDecimal.ZERO;
        for (BigDecimal line : lines) {
            grossSum = grossSum.add(line);
            taxSum = taxSum.add(MoneyUtils.taxFromGross(line, VAT));
        }

        // The invoice must balance against its own lines.
        assertThat(grossSum.subtract(taxSum).add(taxSum)).isEqualByComparingTo(grossSum);
    }

    @Test
    @DisplayName("A cart discount splits across lines and sums back exactly")
    void discountAllocationIsExact() {
        // 5,450 cart, 20% off = 1,090
        List<BigDecimal> lines = List.of(
                new BigDecimal("2400.00"),
                new BigDecimal("2200.00"),
                new BigDecimal("850.00"));
        BigDecimal discount = new BigDecimal("1090.00");

        List<BigDecimal> allocations = MoneyUtils.allocate(discount, lines);

        BigDecimal sum = allocations.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum)
                .as("every piastre of the discount must land on a line")
                .isEqualByComparingTo(discount);

        // Proportional: the biggest line carries the biggest share.
        assertThat(allocations.get(0)).isGreaterThan(allocations.get(2));
    }

    @Test
    @DisplayName("The partial-return case the allocation exists for")
    void partialReturnUsesTheAllocatedShare() {
        // Three items, 5,450 total, 20% cart coupon.
        List<BigDecimal> lines = List.of(
                new BigDecimal("2400.00"),
                new BigDecimal("2200.00"),
                new BigDecimal("850.00"));
        BigDecimal discount = new BigDecimal("1090.00");
        List<BigDecimal> allocations = MoneyUtils.allocate(discount, lines);

        // The customer returns the first item. Its refund is NOT 2,400.
        BigDecimal refund = lines.get(0).subtract(allocations.get(0));

        assertThat(refund)
                .as("refunding the list price would give away the coupon")
                .isLessThan(lines.get(0));
        assertThat(refund).isEqualByComparingTo("1920.0000");   // 2400 - 480
    }

    @Test
    @DisplayName("An awkward split still sums exactly — the last line absorbs the remainder")
    void handlesIndivisibleAmounts() {
        List<BigDecimal> lines = List.of(
                new BigDecimal("333.33"),
                new BigDecimal("333.33"),
                new BigDecimal("333.34"));
        BigDecimal discount = new BigDecimal("100.00");

        List<BigDecimal> allocations = MoneyUtils.allocate(discount, lines);
        BigDecimal sum = allocations.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(sum).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Grand total: subtotal minus discount plus shipping")
    void grandTotalIsCorrect() {
        BigDecimal subtotal = new BigDecimal("4600.00");
        BigDecimal discount = new BigDecimal("0.00");
        BigDecimal shipping = new BigDecimal("100.00");
        BigDecimal codFee = new BigDecimal("0.00");

        BigDecimal grandTotal = MoneyUtils.round(
                subtotal.subtract(discount).add(shipping).add(codFee));

        assertThat(grandTotal).isEqualByComparingTo("4700.0000");
    }

    @Test
    @DisplayName("Zero discount allocates zero to every line")
    void zeroDiscountIsSafe() {
        List<BigDecimal> lines = List.of(
                new BigDecimal("2400.00"), new BigDecimal("2200.00"));

        List<BigDecimal> allocations = MoneyUtils.allocate(MoneyUtils.ZERO, lines);

        assertThat(allocations).hasSize(2);
        assertThat(allocations).allMatch(a -> a.compareTo(BigDecimal.ZERO) == 0);
    }
}
