package com.velora.api.catalog.web;

import com.velora.api.catalog.dto.CategoryDetailResponse;
import com.velora.api.catalog.dto.CategoryTreeResponse;
import com.velora.api.catalog.dto.FilterFacetsResponse;
import com.velora.api.catalog.service.CategoryService;
import com.velora.api.catalog.service.FacetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Categories", description = "Category tree and filter facets")
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final FacetService facetService;

    public CategoryController(CategoryService categoryService, FacetService facetService) {
        this.categoryService = categoryService;
        this.facetService = facetService;
    }

    @Operation(summary = "Full category tree",
            description = "One call for the header mega-menu and the mobile drawer.",
            security = {})
    @GetMapping("/tree")
    public List<CategoryTreeResponse> getTree(HttpServletRequest request) {
        return categoryService.getTree(LocaleResolver.resolve(request));
    }

    @Operation(summary = "Category landing page", security = {})
    @GetMapping("/{slug}")
    public CategoryDetailResponse getBySlug(@PathVariable String slug,
                                            HttpServletRequest request) {
        return categoryService.findBySlug(slug, LocaleResolver.resolve(request));
    }

    @Operation(summary = "Filter facets",
            description = "Brands and attribute values that actually occur in this category.",
            security = {})
    @GetMapping("/filters")
    public FilterFacetsResponse getFilters(@RequestParam(required = false) Long categoryId,
                                           HttpServletRequest request) {
        return facetService.getFacets(categoryId, LocaleResolver.resolve(request));
    }
}
