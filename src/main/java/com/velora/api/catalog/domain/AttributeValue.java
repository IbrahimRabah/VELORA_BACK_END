package com.velora.api.catalog.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "attribute_value")
@Getter
@Setter
@NoArgsConstructor
public class AttributeValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attribute_id", nullable = false)
    private Attribute attribute;

    @Column(name = "code", nullable = false, length = 60)
    private String code;

    /** For colour swatches on the product page. */
    @Column(name = "hex_color", length = 7)
    private String hexColor;

    @Column(name = "display_order", nullable = false)
    private short displayOrder;

    @OneToMany(mappedBy = "key.attributeValueId", cascade = CascadeType.ALL, orphanRemoval = true)
    @MapKey(name = "key.locale")
    private Map<String, AttributeValueTranslation> translations = new LinkedHashMap<>();

    public String nameFor(String locale) {
        AttributeValueTranslation t = translations.get(locale);
        if (t == null) {
            t = translations.get("ar");
        }
        return t == null ? code : t.getName();
    }
}
