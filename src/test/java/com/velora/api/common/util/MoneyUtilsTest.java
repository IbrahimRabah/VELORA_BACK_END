package com.velora.api.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MoneyUtilsTest {

    private static final BigDecimal VAT = new BigDecimal("0.1400");

    @Nested
    @DisplayName("Tax extraction (prices are tax-inclusive)")
    class TaxExtraction {

        @Test
        void extractsNetFromGross() {
            // 1140 gross at 14% -> 1000 net
            BigDecimal net = MoneyUtils.netFromGross(new BigDecimal("1140.00"), VAT);
            assertThat(net).isEqualByComparingTo("1000.0000");
        }

        @Test
        void netPlusTaxAlwaysEqualsGross() {
            BigDecimal gross = new BigDecimal("2400.00");
            BigDecimal net = MoneyUtils.netFromGross(gross, VAT);
            BigDecimal tax = MoneyUtils.taxFromGross(gross, VAT);
            assertThat(net.add(tax)).isEqualByComparingTo(gross);
        }

        @Test
        void zeroTaxRateLeavesAmountUnchanged() {
            BigDecimal gross = new BigDecimal("500.00");
            assertThat(MoneyUtils.netFromGross(gross, BigDecimal.ZERO))
                    .isEqualByComparingTo("500.0000");
            assertThat(MoneyUtils.taxFromGross(gross, BigDecimal.ZERO))
                    .isEqualByComparingTo("0.0000");
        }

        @Test
        void grossFromNetIsTheInverse() {
            BigDecimal net = new BigDecimal("1000.00");
            BigDecimal gross = MoneyUtils.grossFromNet(net, VAT);
            assertThat(gross).isEqualByComparingTo("1140.0000");
            assertThat(MoneyUtils.netFromGross(gross, VAT)).isEqualByComparingTo(net);
        }

        @Test
        void rejectsNegativeAmounts() {
            assertThatThrownBy(() -> MoneyUtils.netFromGross(new BigDecimal("-1"), VAT))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejectsNull() {
            assertThatThrownBy(() -> MoneyUtils.netFromGross(null, VAT))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Per-line tax must be summed, not computed on the total")
    class PerLineTax {

        @Test
        void sumOfLineTaxesMatchesTheInvoice() {
            List<BigDecimal> lines = List.of(
                    new BigDecimal("2400.00"),
                    new BigDecimal("899.99"),
                    new BigDecimal("150.50"));

            BigDecimal taxSum = BigDecimal.ZERO;
            BigDecimal grossSum = BigDecimal.ZERO;
            for (BigDecimal line : lines) {
                taxSum = taxSum.add(MoneyUtils.taxFromGross(line, VAT));
                grossSum = grossSum.add(line);
            }

            BigDecimal netSum = grossSum.subtract(taxSum);
            // The invoice must balance exactly: net + tax == gross
            assertThat(netSum.add(taxSum)).isEqualByComparingTo(grossSum);
        }
    }

    @Nested
    @DisplayName("Cart discount allocation")
    class Allocation {

        @Test
        void allocatesProportionallyToLineTotals() {
            // 20% off a 5000 cart = 1000, split across 3000 / 1500 / 500
            List<BigDecimal> shares = MoneyUtils.allocate(
                    new BigDecimal("1000.00"),
                    List.of(new BigDecimal("3000"), new BigDecimal("1500"), new BigDecimal("500")));

            assertThat(shares).hasSize(3);
            assertThat(shares.get(0)).isEqualByComparingTo("600.0000");
            assertThat(shares.get(1)).isEqualByComparingTo("300.0000");
            assertThat(shares.get(2)).isEqualByComparingTo("100.0000");
        }

        @Test
        void sumOfSharesAlwaysEqualsTheTotalDespiteRounding() {
            // 100 / 3 does not divide evenly — the last line must absorb the remainder
            List<BigDecimal> shares = MoneyUtils.allocate(
                    new BigDecimal("100.00"),
                    List.of(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE));

            BigDecimal sum = shares.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(sum).isEqualByComparingTo("100.00");
        }

        @Test
        void handlesAwkwardAmountsWithoutLosingAPiastre() {
            List<BigDecimal> shares = MoneyUtils.allocate(
                    new BigDecimal("33.33"),
                    List.of(new BigDecimal("7"), new BigDecimal("11"), new BigDecimal("13")));

            BigDecimal sum = shares.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(sum).isEqualByComparingTo("33.33");
        }

        @Test
        void returnsEmptyListForNoWeights() {
            assertThat(MoneyUtils.allocate(new BigDecimal("100"), List.of())).isEmpty();
            assertThat(MoneyUtils.allocate(new BigDecimal("100"), null)).isEmpty();
        }

        @Test
        void doesNotLoseTheAmountWhenAllWeightsAreZero() {
            List<BigDecimal> shares = MoneyUtils.allocate(
                    new BigDecimal("50.00"),
                    List.of(BigDecimal.ZERO, BigDecimal.ZERO));

            BigDecimal sum = shares.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(sum).isEqualByComparingTo("50.00");
        }

        @Test
        void singleLineReceivesEverything() {
            List<BigDecimal> shares = MoneyUtils.allocate(
                    new BigDecimal("75.25"), List.of(new BigDecimal("999")));
            assertThat(shares).hasSize(1);
            assertThat(shares.get(0)).isEqualByComparingTo("75.25");
        }
    }

    @Nested
    @DisplayName("Line arithmetic")
    class LineArithmetic {

        @Test
        void multipliesUnitPriceByQuantity() {
            assertThat(MoneyUtils.lineTotal(new BigDecimal("2400.00"), 3))
                    .isEqualByComparingTo("7200.0000");
        }

        @Test
        void zeroQuantityGivesZero() {
            assertThat(MoneyUtils.lineTotal(new BigDecimal("2400.00"), 0))
                    .isEqualByComparingTo("0.0000");
        }

        @Test
        void rejectsNegativeQuantity() {
            assertThatThrownBy(() -> MoneyUtils.lineTotal(new BigDecimal("10"), -1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void calculatesPercentage() {
            assertThat(MoneyUtils.percentOf(new BigDecimal("2000"), new BigDecimal("20")))
                    .isEqualByComparingTo("400.0000");
        }
    }

    @Test
    void roundsHalfUpAtStorageScale() {
        assertThat(MoneyUtils.round(new BigDecimal("10.00005"))).isEqualByComparingTo("10.0001");
        assertThat(MoneyUtils.roundForDisplay(new BigDecimal("10.555"))).isEqualByComparingTo("10.56");
    }
}
