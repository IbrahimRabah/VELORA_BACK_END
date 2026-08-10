package com.velora.api.invoice.web;

import com.velora.api.identity.security.UserPrincipal;
import com.velora.api.invoice.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "My Invoices", description = "The customer's own invoices")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/me/invoices")
public class CustomerInvoiceController {

    private final InvoiceService invoiceService;

    public CustomerInvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @Operation(summary = "Download my invoice",
            description = """
                    Scoped by customer. An invoice number belonging to someone else
                    returns the same 404 as one that does not exist, so the endpoint
                    cannot be used to discover invoice numbers.
                    """)
    @GetMapping("/{invoiceNumber}/pdf")
    public ResponseEntity<byte[]> pdf(@AuthenticationPrincipal UserPrincipal principal,
                                      @PathVariable String invoiceNumber) {

        byte[] body = invoiceService.renderPdfForCustomer(principal.id(), invoiceNumber);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(invoiceNumber + ".pdf").build().toString())
                .body(body);
    }
}
