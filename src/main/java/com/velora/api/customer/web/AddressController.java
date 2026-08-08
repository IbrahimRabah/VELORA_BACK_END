package com.velora.api.customer.web;

import com.velora.api.catalog.web.LocaleResolver;
import com.velora.api.customer.dto.AddressRequest;
import com.velora.api.customer.dto.AddressResponse;
import com.velora.api.customer.service.AddressService;
import com.velora.api.identity.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Addresses", description = "The customer's address book")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/me/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @Operation(summary = "List saved addresses", description = "Default first")
    @GetMapping
    public List<AddressResponse> list(@AuthenticationPrincipal UserPrincipal principal,
                                      HttpServletRequest request) {
        return addressService.list(principal.id(), LocaleResolver.resolve(request));
    }

    @Operation(summary = "Add an address",
            description = "The first address saved becomes the default automatically.")
    @PostMapping
    public ResponseEntity<AddressResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddressRequest body,
            HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(
                addressService.create(principal.id(), body, LocaleResolver.resolve(request)));
    }

    @Operation(summary = "Update an address")
    @PutMapping("/{addressId}")
    public AddressResponse update(@AuthenticationPrincipal UserPrincipal principal,
                                  @PathVariable Long addressId,
                                  @Valid @RequestBody AddressRequest body,
                                  HttpServletRequest request) {

        return addressService.update(principal.id(), addressId, body,
                LocaleResolver.resolve(request));
    }

    @Operation(summary = "Delete an address",
            description = "Soft delete. Past orders keep their own copy of the address.")
    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal principal,
                                       @PathVariable Long addressId) {
        addressService.delete(principal.id(), addressId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Make this the default")
    @PutMapping("/{addressId}/default")
    public AddressResponse setDefault(@AuthenticationPrincipal UserPrincipal principal,
                                      @PathVariable Long addressId,
                                      HttpServletRequest request) {
        return addressService.setDefault(principal.id(), addressId,
                LocaleResolver.resolve(request));
    }
}
