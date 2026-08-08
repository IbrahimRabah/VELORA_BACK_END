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

@Entity
@Table(name = "attribute_value_translation")
@Getter
@Setter
@NoArgsConstructor
public class AttributeValueTranslation {

    @EmbeddedId
    private Key key = new Key();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("attributeValueId")
    @JoinColumn(name = "attribute_value_id", nullable = false)
    private AttributeValue attributeValue;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    public void attachTo(AttributeValue parent, String locale) {
        this.attributeValue = parent;
        this.key.setLocale(locale);
    }

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Column(name = "attribute_value_id")
        private Long attributeValueId;

        @Column(name = "locale", length = 5)
        private String locale;
    }
}
