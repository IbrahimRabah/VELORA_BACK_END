package com.velora.api.shipping;

import static org.assertj.core.api.Assertions.assertThat;

import com.velora.api.shipping.domain.ShippingRate;
import com.velora.api.shipping.domain.ShippingZone;
import com.velora.api.shipping.service.ShippingCalculator;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ShippingCalculatorTest {

    private ShippingCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new ShippingCalculator();
    }

    @Nested
    @DisplayName("Flat rate — VELORA's current pricing")
    class FlatRate {

        @Test
        @DisplayName("Cairo and Lower Egypt: 70 EGP regardless of order value")
        void lowerEgyptIsSeventy() {
            ShippingRate rate = flatRate("70.00");

            var cheap = calculator.calculate(rate, new BigDecimal("500"), 200, true);
            var expensive = calculator.calculate(rate, new BigDecimal("50000"), 200, true);

            assertThat(cheap.shippingCost()).isEqualByComparingTo("70.00");
            assertThat(expensive.shippingCost()).isEqualByComparingTo("70.00");
        }

        @Test
        @DisplayName("Upper Egypt: 100 EGP")
        void upperEgyptIsOneHundred() {
            var result = calculator.calculate(
                    flatRate("100.00"), new BigDecimal("2400"), 240, true);

            assertThat(result.shippingCost()).isEqualByComparingTo("100.00");
            assertThat(result.freeShippingApplied()).isFalse();
            assertThat(result.freeShippingThreshold()).isNull();
        }

        @Test
        @DisplayName("Weight is ignored when the rate is flat")
        void weightIsIgnored() {
            ShippingRate rate = flatRate("70.00");

            var light = calculator.calculate(rate, new BigDecimal("1000"), 100, true);
            var heavy = calculator.calculate(rate, new BigDecimal("1000"), 25000, true);

            assertThat(heavy.shippingCost()).isEqualByComparingTo(light.shippingCost());
        }

        @Test
        void codFeeIsZeroToday() {
            var result = calculator.calculate(
                    flatRate("70.00"), new BigDecimal("1000"), 200, true);

            assertThat(result.codFee()).isEqualByComparingTo("0.00");
            assertThat(result.totalDeliveryCharge()).isEqualByComparingTo("70.00");
        }
    }

    @Nested
    @DisplayName("Free-shipping threshold — configured but off today")
    class FreeShipping {

        @Test
        void appliesAtOrAboveTheThreshold() {
            ShippingRate rate = flatRate("70.00");
            rate.setFreeShippingOver(new BigDecimal("2000"));

            var exactly = calculator.calculate(rate, new BigDecimal("2000"), 200, true);
            var above = calculator.calculate(rate, new BigDecimal("2500"), 200, true);

            assertThat(exactly.shippingCost()).isEqualByComparingTo("0.00");
            assertThat(exactly.freeShippingApplied()).isTrue();
            assertThat(above.freeShippingApplied()).isTrue();
        }

        @Test
        @DisplayName("Below the threshold, reports how much more to spend")
        void reportsTheGap() {
            ShippingRate rate = flatRate("70.00");
            rate.setFreeShippingOver(new BigDecimal("2000"));

            var result = calculator.calculate(rate, new BigDecimal("1750"), 200, true);

            assertThat(result.shippingCost()).isEqualByComparingTo("70.00");
            assertThat(result.freeShippingApplied()).isFalse();
            // "Spend 250 more for free delivery" is worth real money in conversion.
            assertThat(result.amountToFreeShipping()).isEqualByComparingTo("250.00");
        }

        @Test
        @DisplayName("baseCost still reports what shipping would have cost")
        void keepsTheUndiscountedCost() {
            ShippingRate rate = flatRate("70.00");
            rate.setFreeShippingOver(new BigDecimal("2000"));

            var result = calculator.calculate(rate, new BigDecimal("3000"), 200, true);

            assertThat(result.shippingCost()).isEqualByComparingTo("0.00");
            assertThat(result.baseCost()).isEqualByComparingTo("70.00");
        }
    }

    @Nested
    @DisplayName("Weight tiers — ready, not switched on")
    class WeightBased {

        @Test
        void noSurchargeWithinTheIncludedWeight() {
            var result = calculator.calculate(
                    weightRate("70.00", 1000, "15.00"), new BigDecimal("1000"), 900, true);

            assertThat(result.shippingCost()).isEqualByComparingTo("70.00");
        }

        @Test
        @DisplayName("Part of a kilo is billed as a whole one, like couriers do")
        void roundsPartialKilosUp() {
            ShippingRate rate = weightRate("70.00", 1000, "15.00");

            // 1,200 g = 200 g over = 1 chargeable kilo
            var justOver = calculator.calculate(rate, new BigDecimal("1000"), 1200, true);
            assertThat(justOver.shippingCost()).isEqualByComparingTo("85.00");

            // 3,100 g = 2,100 g over = 3 chargeable kilos
            var heavier = calculator.calculate(rate, new BigDecimal("1000"), 3100, true);
            assertThat(heavier.shippingCost()).isEqualByComparingTo("115.00");
        }

        @Test
        @DisplayName("Free shipping waives the weight surcharge too")
        void freeShippingCoversTheSurcharge() {
            ShippingRate rate = weightRate("70.00", 1000, "15.00");
            rate.setFreeShippingOver(new BigDecimal("2000"));

            var result = calculator.calculate(rate, new BigDecimal("2500"), 5000, true);

            assertThat(result.shippingCost()).isEqualByComparingTo("0.00");
            assertThat(result.baseCost()).isEqualByComparingTo("130.00");
        }
    }

    // ------------------------------------------------------------------ helpers

    private ShippingRate flatRate(String cost) {
        ShippingZone zone = new ShippingZone();
        zone.setCode("TEST");
        zone.setNameAr("اختبار");
        zone.setNameEn("Test");

        ShippingRate rate = new ShippingRate();
        rate.setZone(zone);
        rate.setBaseCost(new BigDecimal(cost));
        rate.setCostPerExtraKg(BigDecimal.ZERO);
        rate.setCodFee(BigDecimal.ZERO);
        return rate;
    }

    private ShippingRate weightRate(String cost, int includedGrams, String perKg) {
        ShippingRate rate = flatRate(cost);
        rate.setMaxWeightGrams(includedGrams);
        rate.setCostPerExtraKg(new BigDecimal(perKg));
        return rate;
    }
}
