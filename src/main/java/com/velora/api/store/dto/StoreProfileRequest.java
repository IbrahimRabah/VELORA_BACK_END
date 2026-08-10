package com.velora.api.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "The seller details printed on every invoice")
public record StoreProfileRequest(

        @Schema(example = "VELORA")
        @NotBlank(message = "Legal name is required")
        @Size(max = 200) String legalName,

        @Size(max = 200) String legalNameEn,

        @Schema(example = "القاهرة، مصر")
        @Size(max = 500) String address,

        @Schema(example = "01090386165")
        @Size(max = 30) String phone,

        @Email(message = "Not a valid email address")
        @Size(max = 255) String email,

        @Schema(example = "123-456-789",
                description = "Egyptian tax registration number. Leave empty until "
                        + "registered — a blank line is omitted from the invoice, and "
                        + "that is far better than printing a wrong number.")
        @Pattern(regexp = "^$|^[0-9]{3}-?[0-9]{3}-?[0-9]{3}$",
                message = "Tax number must be nine digits, e.g. 123-456-789")
        @Size(max = 30) String taxNumber,

        @Size(max = 30) String commercialRegister,

        @Size(max = 255) String website,

        @Schema(example = "شكراً لتسوقكم من فيلورا")
        @Size(max = 500) String invoiceFooterNote
) {
}
