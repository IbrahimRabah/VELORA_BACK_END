package com.velora.api.catalog.web.admin;

import com.velora.api.catalog.dto.admin.VariantAdminResponse;
import com.velora.api.catalog.dto.admin.VariantMatrixPreviewResponse;
import com.velora.api.catalog.dto.admin.VariantMatrixRequest;
import com.velora.api.catalog.dto.admin.VariantSaveRequest;
import com.velora.api.catalog.service.admin.VariantAdminService;
import com.velora.api.identity.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin — Variants", description = "The sellable units. Requires ROLE_ADMIN.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/admin")
public class AdminVariantController {

    private final VariantAdminService variantService;

    public AdminVariantController(VariantAdminService variantService) {
        this.variantService = variantService;
    }

    @Operation(summary = "List a product's variants")
    @GetMapping("/products/{productId}/variants")
    public List<VariantAdminResponse> list(@PathVariable Long productId) {
        return variantService.listForProduct(productId);
    }

    @Operation(summary = "Preview the variant matrix",
            description = """
                    Returns every combination of the selected attribute values WITHOUT saving.
                    Review it, remove what you are not stocking, then POST to /variants.

                    Two colours and three sizes give six variants. Four attributes with five
                    values each give 625 — which is why nothing is saved at this step.
                    """)
    @PostMapping("/products/{productId}/variants/preview")
    public VariantMatrixPreviewResponse preview(@PathVariable Long productId,
                                                @Valid @RequestBody VariantMatrixRequest request) {
        return variantService.previewMatrix(productId, request, "ar");
    }

    @Operation(summary = "Create or update variants",
            description = """
                    Used both after a matrix preview and when adding a single new colour later.
                    Set `id` to update, leave it null to create. Existing combinations are skipped.

                    Every created variant also gets an inventory row — without one it would
                    report zero stock forever.
                    """)
    @PostMapping("/products/{productId}/variants")
    public List<VariantAdminResponse> save(@PathVariable Long productId,
                                           @Valid @RequestBody VariantSaveRequest request,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        return variantService.saveVariants(productId, request,
                principal == null ? null : principal.id());
    }

    @Operation(summary = "Archive a variant",
            description = "Soft delete. The SKU is never reused.")
    @DeleteMapping("/variants/{variantId}")
    public ResponseEntity<Void> archive(@PathVariable Long variantId) {
        variantService.archive(variantId);
        return ResponseEntity.noContent().build();
    }
}
