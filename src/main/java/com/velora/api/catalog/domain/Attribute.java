package com.velora.api.catalog.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A product characteristic — colour, case size, movement, water resistance.
 *
 * <p>{@code variantDefining} is the important flag and it is not cosmetic:
 * <ul>
 *   <li><b>true</b> — generates the selector controls on the product page and
 *       multiplies the number of SKUs. Colour, size.</li>
 *   <li><b>false</b> — appears in the specification table and drives filters,
 *       but never creates a new SKU. Movement, water resistance, fragrance notes.</li>
 * </ul>
 */
@Entity
@Table(name = "attribute")
@Getter
@Setter
@NoArgsConstructor
public class Attribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 20)
    private AttributeDataType dataType = AttributeDataType.LIST;

    @Column(name = "is_variant_defining", nullable = false)
    private boolean variantDefining;

    @Column(name = "is_filterable", nullable = false)
    private boolean filterable = true;

    @Column(name = "display_order", nullable = false)
    private short displayOrder;

    @OneToMany(mappedBy = "attribute", cascade = CascadeType.ALL, orphanRemoval = true)
    @MapKey(name = "key.locale")
    private Map<String, AttributeTranslation> translations = new LinkedHashMap<>();

    @OneToMany(mappedBy = "attribute", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    private List<AttributeValue> values = new ArrayList<>();

    public String nameFor(String locale) {
        AttributeTranslation t = translations.get(locale);
        if (t == null) {
            t = translations.get("ar");
        }
        return t == null ? code : t.getName();
    }
}
