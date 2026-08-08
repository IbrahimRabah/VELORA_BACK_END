package com.velora.api.catalog.service.admin;

import com.velora.api.catalog.domain.Attribute;
import com.velora.api.catalog.domain.AttributeDataType;
import com.velora.api.catalog.domain.AttributeTranslation;
import com.velora.api.catalog.domain.AttributeValue;
import com.velora.api.catalog.domain.AttributeValueTranslation;
import com.velora.api.catalog.domain.Brand;
import com.velora.api.catalog.domain.Category;
import com.velora.api.catalog.domain.CategoryTranslation;
import com.velora.api.catalog.dto.admin.AttributeSaveRequest;
import com.velora.api.catalog.dto.admin.BrandSaveRequest;
import com.velora.api.catalog.dto.admin.CategorySaveRequest;
import com.velora.api.catalog.dto.admin.TranslationRequest;
import com.velora.api.catalog.repository.AttributeRepository;
import com.velora.api.catalog.repository.BrandRepository;
import com.velora.api.catalog.repository.CategoryRepository;
import com.velora.api.common.exception.BusinessException;
import com.velora.api.common.exception.ErrorCode;
import com.velora.api.common.util.SlugGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Categories, brands and attributes — the structures products hang off.
 *
 * <p>These change rarely and are set up once, but getting the
 * {@code variantDefining} flag wrong on an attribute is expensive to undo, so the
 * service refuses to flip it once variants depend on it.
 */
@Service
public class TaxonomyAdminService {

    private static final Logger log = LoggerFactory.getLogger(TaxonomyAdminService.class);

    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final AttributeRepository attributeRepository;

    public TaxonomyAdminService(CategoryRepository categoryRepository,
                                BrandRepository brandRepository,
                                AttributeRepository attributeRepository) {
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.attributeRepository = attributeRepository;
    }

    // ----------------------------------------------------------------- category

    @Transactional
    public Long createCategory(CategorySaveRequest request) {
        Category category = new Category();
        applyCategory(category, request, true);
        Category saved = categoryRepository.save(category);
        log.info("Created category id={} slug={}", saved.getId(), saved.getSlug());
        return saved.getId();
    }

    @Transactional
    public Long updateCategory(Long id, CategorySaveRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Category not found"));
        applyCategory(category, request, false);
        return categoryRepository.save(category).getId();
    }

    private void applyCategory(Category category, CategorySaveRequest request, boolean isNew) {
        if (request.parentId() != null) {
            Category parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                            "Parent category not found"));
            // A category cannot be its own ancestor.
            if (category.getId() != null && parent.getId().equals(category.getId())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "A category cannot be its own parent");
            }
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        if (isNew || request.slug() != null) {
            String source = request.slug() != null && !request.slug().isBlank()
                    ? request.slug()
                    : request.translations().get(0).name();
            category.setSlug(SlugGenerator.generateUnique(source,
                    s -> !categoryRepository.existsBySlug(s)));
        }

        category.setImageUrl(request.imageUrl());
        category.setBannerUrl(request.bannerUrl());
        if (request.displayOrder() != null) {
            category.setDisplayOrder(request.displayOrder().shortValue());
        }
        if (request.active() != null) {
            category.setActive(request.active());
        }

        category.getTranslations().clear();
        for (TranslationRequest t : request.translations()) {
            CategoryTranslation translation = new CategoryTranslation();
            // @MapsId derives category_id from this association. Setting the id
            // directly fails on create, because the category has no id yet.
            translation.attachTo(category, t.locale());
            translation.setName(t.name());
            translation.setDescription(t.description());
            translation.setMetaTitle(t.metaTitle());
            translation.setMetaDescription(t.metaDescription());
            category.getTranslations().put(t.locale(), translation);
        }
    }

    // -------------------------------------------------------------------- brand

    @Transactional
    public Long saveBrand(Long id, BrandSaveRequest request) {
        Brand brand = id == null
                ? new Brand()
                : brandRepository.findById(id).orElseThrow(
                        () -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Brand not found"));

        if (id == null || request.slug() != null) {
            String source = request.slug() != null && !request.slug().isBlank()
                    ? request.slug() : request.nameEn();
            brand.setSlug(SlugGenerator.generate(source));
        }
        brand.setNameAr(request.nameAr());
        brand.setNameEn(request.nameEn());
        brand.setLogoUrl(request.logoUrl());
        if (request.active() != null) {
            brand.setActive(request.active());
        }
        return brandRepository.save(brand).getId();
    }

    // ---------------------------------------------------------------- attribute

    @Transactional
    public Long saveAttribute(Long id, AttributeSaveRequest request) {
        Attribute attribute = id == null
                ? new Attribute()
                : attributeRepository.findById(id).orElseThrow(
                        () -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                                "Attribute not found"));

        // Flipping this after variants exist would orphan every SKU built from it.
        if (id != null && attribute.isVariantDefining() != request.variantDefining()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "variantDefining cannot be changed once the attribute is in use. "
                            + "Create a new attribute instead.");
        }

        attribute.setCode(request.code());
        attribute.setDataType(request.dataType() == null
                ? AttributeDataType.LIST : AttributeDataType.valueOf(request.dataType()));
        attribute.setVariantDefining(request.variantDefining());
        attribute.setFilterable(request.filterable());
        if (request.displayOrder() != null) {
            attribute.setDisplayOrder(request.displayOrder().shortValue());
        }

        attribute.getTranslations().clear();
        for (var t : request.translations()) {
            AttributeTranslation translation = new AttributeTranslation();
            translation.attachTo(attribute, t.locale());
            translation.setName(t.name());
            attribute.getTranslations().put(t.locale(), translation);
        }

        if (request.values() != null) {
            applyValues(attribute, request);
        }

        Attribute saved = attributeRepository.save(attribute);
        log.info("Saved attribute id={} code={} variantDefining={}",
                saved.getId(), saved.getCode(), saved.isVariantDefining());
        return saved.getId();
    }

    private void applyValues(Attribute attribute, AttributeSaveRequest request) {
        for (var valueRequest : request.values()) {
            AttributeValue value = attribute.getValues().stream()
                    .filter(v -> valueRequest.id() != null && valueRequest.id().equals(v.getId()))
                    .findFirst()
                    .orElseGet(() -> {
                        AttributeValue created = new AttributeValue();
                        created.setAttribute(attribute);
                        attribute.getValues().add(created);
                        return created;
                    });

            value.setCode(valueRequest.code());
            value.setHexColor(valueRequest.hexColor());
            if (valueRequest.displayOrder() != null) {
                value.setDisplayOrder(valueRequest.displayOrder().shortValue());
            }

            value.getTranslations().clear();
            for (var t : valueRequest.translations()) {
                AttributeValueTranslation translation = new AttributeValueTranslation();
                translation.attachTo(value, t.locale());
                translation.setName(t.name());
                value.getTranslations().put(t.locale(), translation);
            }
        }
    }
}
