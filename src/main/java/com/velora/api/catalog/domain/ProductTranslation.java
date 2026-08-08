package com.velora.api.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product_translation")
@Getter
@Setter
@NoArgsConstructor
public class ProductTranslation {

    @EmbeddedId
    private Key key;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(name = "description")
    private String description;

    @Column(name = "meta_title", length = 255)
    private String metaTitle;

    @Column(name = "meta_description", length = 500)
    private String metaDescription;

    /**
     * Arabic-normalized copy used ONLY for searching.
     * Always write it through {@code ArabicNormalizer.normalize()} — and normalize
     * the incoming query with the same function, or search stops matching.
     */
    @Column(name = "search_text", length = 1000)
    private String searchText;

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

        @Column(name = "locale", length = 5)
        private String locale;
    }
}
