package com.velora.api.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An informational specification on the PRODUCT — movement, water resistance,
 * fragrance notes. Never creates a SKU; that is what
 * {@link VariantAttributeValue} is for.
 */
@Entity
@Table(name = "product_attribute_value")
@Getter
@Setter
@NoArgsConstructor
public class ProductAttributeValue {

    @EmbeddedId
    private Key key;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("productId")
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("attributeId")
    @JoinColumn(name = "attribute_id", nullable = false)
    private Attribute attribute;

    /** Set for LIST attributes. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_value_id")
    private AttributeValue attributeValue;

    /** Set for TEXT and NUMBER attributes instead. */
    @Column(name = "value_text", length = 500)
    private String valueText;

    public String displayValue(String locale) {
        return attributeValue != null ? attributeValue.nameFor(locale) : valueText;
    }

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Column(name = "product_id")
        private Long productId;

        @Column(name = "attribute_id")
        private Long attributeId;
    }
}
