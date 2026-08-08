package com.velora.api.shipping.service;

import com.velora.api.common.util.MoneyUtils;
import com.velora.api.shipping.domain.ShippingRate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/**
 * Turns a rate configuration plus an order into a delivery charge.
 *
 * <p>Deliberately NOT a Strategy hierarchy. Flat rates, weight tiers and free
 * thresholds are all the same calculation with different numbers, and splitting
 * them into classes would produce three files that each read a different subset of
 * the same row.
 *
 * <p>The Strategy pattern earns its place when the calculation <em>source</em>
 * differs — a courier API quoting live prices is a genuinely different thing, and
 * that is when this becomes an interface with two implementations.
 */
@Component
public class ShippingCalculator {

    private static final BigDecimal GRAMS_PER_KG = BigDecimal.valueOf(1000);

    /**
     * @param subtotal     order value, tax-inclusive
     * @param weightGrams  total cart weight
     * @param codApplies   whether cash will be collected on delivery
     */
    public Calculation calculate(ShippingRate rate, BigDecimal subtotal,
                                 int weightGrams, boolean codApplies) {

        BigDecimal baseCost = rate.getBaseCost();
        BigDecimal cost = baseCost;

        // Weight surcharge, if the rate is configured for it.
        if (rate.isWeightBased() && weightGrams > rate.getMaxWeightGrams()) {
            int excessGrams = weightGrams - rate.getMaxWeightGrams();
            // Part of a kilo is charged as a whole one — that is how couriers bill.
            BigDecimal extraKg = BigDecimal.valueOf(excessGrams)
                    .divide(GRAMS_PER_KG, 0, RoundingMode.CEILING);
            cost = cost.add(rate.getCostPerExtraKg().multiply(extraKg));
        }

        BigDecimal preDiscountCost = MoneyUtils.round(cost);

        // Free shipping is applied to the whole charge, weight surcharge included.
        boolean freeApplied = false;
        if (rate.hasFreeThreshold()
                && subtotal.compareTo(rate.getFreeShippingOver()) >= 0) {
            cost = BigDecimal.ZERO;
            freeApplied = true;
        }

        BigDecimal amountToFree = null;
        if (rate.hasFreeThreshold() && !freeApplied) {
            amountToFree = MoneyUtils.round(rate.getFreeShippingOver().subtract(subtotal));
        }

        BigDecimal codFee = codApplies ? rate.getCodFee() : BigDecimal.ZERO;

        return new Calculation(
                MoneyUtils.round(cost),
                preDiscountCost,
                MoneyUtils.round(codFee),
                freeApplied,
                rate.getFreeShippingOver(),
                amountToFree);
    }

    /** The outcome of one shipping calculation. */
    public record Calculation(
            BigDecimal shippingCost,
            BigDecimal baseCost,
            BigDecimal codFee,
            boolean freeShippingApplied,
            BigDecimal freeShippingThreshold,
            BigDecimal amountToFreeShipping
    ) {

        public BigDecimal totalDeliveryCharge() {
            return MoneyUtils.round(shippingCost.add(codFee));
        }
    }
}
