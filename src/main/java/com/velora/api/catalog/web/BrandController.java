package com.velora.api.catalog.web;

import com.velora.api.catalog.dto.BrandResponse;
import com.velora.api.catalog.mapper.CatalogMapper;
import com.velora.api.catalog.repository.BrandRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Brands", description = "Active brands")
@RestController
@RequestMapping("/api/v1/brands")
public class BrandController {

    private final BrandRepository brandRepository;
    private final CatalogMapper mapper;

    public BrandController(BrandRepository brandRepository, CatalogMapper mapper) {
        this.brandRepository = brandRepository;
        this.mapper = mapper;
    }

    @Operation(summary = "List active brands", security = {})
    @Transactional(readOnly = true)
    @GetMapping
    public List<BrandResponse> list(HttpServletRequest request) {
        String locale = LocaleResolver.resolve(request);
        return brandRepository.findByActiveTrueOrderByNameArAsc().stream()
                .map(brand -> mapper.toBrand(brand, locale))
                .toList();
    }
}
