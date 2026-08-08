package com.velora.api.shipping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * What shipping costs and when it arrives.
 *
 * <p>The delivery estimate matters more than it looks: showing it before checkout
 * removes more support messages than almost any other single field.
 */
@Schema(description = "Shipping cost and delivery estimate")
public record ShippingQuoteResponse(
        Long governorateId,
        String governorateName,
        String zoneName,

        @Schema(example = "70.00", description = "What the customer pays for delivery")
        BigDecimal shippingCost,

        @Schema(description = "The rate before any free-shipping discount")
        BigDecimal baseCost,

        @Schema(description = "Extra charge for cash collection. Zero today.")
        BigDecimal codFee,

        @Schema(description = "True when the order value earned free delivery")
        boolean freeShippingApplied,

        @Schema(description = "Spend this much for free delivery. Null when not offered.")
        BigDecimal freeShippingThreshold,

        @Schema(description = "How much more to spend to reach it. Null when not offered.")
        BigDecimal amountToFreeShipping,

        int deliveryDaysMin,
        int deliveryDaysMax,

        @Schema(description = "Cart total used in the calculation")
        BigDecimal orderSubtotal,

        @Schema(description = "Cart weight in grams, for weight-based rates")
        int totalWeightGrams,

        @Schema(description = "subtotal + shipping + COD fee")
        BigDecimal estimatedTotal
) {
}
