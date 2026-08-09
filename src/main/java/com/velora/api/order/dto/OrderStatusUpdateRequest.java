package com.velora.api.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Move an order to a new fulfilment status")
public record OrderStatusUpdateRequest(

        @Schema(example = "SHIPPED",
                allowableValues = {"CONFIRMED", "PROCESSING", "SHIPPED", "OUT_FOR_DELIVERY",
                        "DELIVERED", "DELIVERY_FAILED", "REFUSED_ON_DELIVERY",
                        "RETURNED_TO_SELLER", "CANCELLED"})
        @NotBlank(message = "The target status is required")
        String status,

        @Schema(description = "Required for failures and refusals",
                example = "العميل لم يرد على الهاتف")
        @Size(max = 500) String note
) {
}
