package com.velora.api.catalog.web;

import com.velora.api.catalog.repository.ProductVariantRepository;
import com.velora.api.common.exception.BusinessException;
import com.velora.api.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Variants", description = "Live stock for a single variant")
@RestController
@RequestMapping("/api/v1/variants")
public class VariantController {

    private final ProductVariantRepository variantRepository;

    public VariantController(ProductVariantRepository variantRepository) {
        this.variantRepository = variantRepository;
    }

    /**
     * Called when the customer changes colour on the product page, and again just
     * before add-to-cart. The detail response already carries stock, but it may be
     * seconds old on a busy product.
     */
    @Operation(summary = "Current availability of one variant", security = {})
    @Transactional(readOnly = true)
    @GetMapping("/{id}/availability")
    public Map<String, Object> availability(@PathVariable Long id) {
        var variant = variantRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.VARIANT_NOT_FOUND));

        return Map.of(
                "variantId", variant.getId(),
                "sku", variant.getSku(),
                "price", variant.getPrice(),
                "availableQty", variant.getAvailable(),
                "inStock", variant.isInStock(),
                "sellable", variant.isSellable());
    }
}
