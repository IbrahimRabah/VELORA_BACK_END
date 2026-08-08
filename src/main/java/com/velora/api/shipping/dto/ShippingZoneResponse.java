package com.velora.api.shipping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "A zone with its rate and the governorates it covers")
public record ShippingZoneResponse(
        Long zoneId,
        String code,
        String nameAr,
        String nameEn,
        BigDecimal baseCost,
        BigDecimal freeShippingOver,
        BigDecimal codFee,
        int deliveryDaysMin,
        int deliveryDaysMax,
        boolean active,
        List<String> governorates
) {
}
