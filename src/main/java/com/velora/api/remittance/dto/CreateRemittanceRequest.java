package com.velora.api.remittance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Record cash handed over by the courier")
public record CreateRemittanceRequest(

        @Schema(example = "بوسطة")
        @NotBlank(message = "Courier name is required")
        @Size(max = 150) String courierName,

        @Schema(description = "The courier's own batch reference, so both sides can "
                + "find the same settlement")
        @Size(max = 100) String courierReference,

        @Schema(description = "Defaults to today")
        LocalDate settlementDate,

        @Schema(description = "The orders this batch covers")
        @NotEmpty(message = "Select at least one order")
        List<Long> orderIds,

        @Schema(example = "9500.00", description = "What actually arrived — not what "
                + "was expected. The difference is the point.")
        @NotNull(message = "Received amount is required")
        @DecimalMin(value = "0.0", message = "Amount cannot be negative")
        BigDecimal receivedAmount,

        @Schema(example = "طلب VLR-260810-1234 مرتجع ولم يُحصّل",
                description = "REQUIRED when the amounts do not match")
        @Size(max = 1000) String note
) {
}
