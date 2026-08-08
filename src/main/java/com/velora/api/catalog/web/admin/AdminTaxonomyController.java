package com.velora.api.catalog.web.admin;

import com.velora.api.catalog.dto.admin.AttributeSaveRequest;
import com.velora.api.catalog.dto.admin.BrandSaveRequest;
import com.velora.api.catalog.dto.admin.CategorySaveRequest;
import com.velora.api.catalog.service.admin.TaxonomyAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin — Taxonomy",
        description = "Categories, brands and attributes. Requires ROLE_ADMIN.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/admin")
public class AdminTaxonomyController {

    private final TaxonomyAdminService taxonomyService;

    public AdminTaxonomyController(TaxonomyAdminService taxonomyService) {
        this.taxonomyService = taxonomyService;
    }

    @Operation(summary = "Create a category")
    @PostMapping("/categories")
    public ResponseEntity<Map<String, Long>> createCategory(
            @Valid @RequestBody CategorySaveRequest request) {
        Long id = taxonomyService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id));
    }

    @Operation(summary = "Update a category")
    @PutMapping("/categories/{id}")
    public Map<String, Long> updateCategory(@PathVariable Long id,
                                            @Valid @RequestBody CategorySaveRequest request) {
        return Map.of("id", taxonomyService.updateCategory(id, request));
    }

    @Operation(summary = "Create a brand")
    @PostMapping("/brands")
    public ResponseEntity<Map<String, Long>> createBrand(
            @Valid @RequestBody BrandSaveRequest request) {
        Long id = taxonomyService.saveBrand(null, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id));
    }

    @Operation(summary = "Update a brand")
    @PutMapping("/brands/{id}")
    public Map<String, Long> updateBrand(@PathVariable Long id,
                                         @Valid @RequestBody BrandSaveRequest request) {
        return Map.of("id", taxonomyService.saveBrand(id, request));
    }

    @Operation(summary = "Create an attribute",
            description = """
                    Set `variantDefining` carefully:
                    TRUE generates SKUs (colour, size) — FALSE is specification only
                    (movement, water resistance, fragrance notes).

                    It cannot be changed once variants depend on the attribute.
                    """)
    @PostMapping("/attributes")
    public ResponseEntity<Map<String, Long>> createAttribute(
            @Valid @RequestBody AttributeSaveRequest request) {
        Long id = taxonomyService.saveAttribute(null, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id));
    }

    @Operation(summary = "Update an attribute or add values")
    @PutMapping("/attributes/{id}")
    public Map<String, Long> updateAttribute(@PathVariable Long id,
                                             @Valid @RequestBody AttributeSaveRequest request) {
        return Map.of("id", taxonomyService.saveAttribute(id, request));
    }
}
