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
@Table(name = "product_translation")
@Getter
@Setter
@NoArgsConstructor
public class ProductTranslation {

    @EmbeddedId
    private Key key = new Key();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("productId")
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

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
     * Arabic-normalized copy used ONLY for searching. Always written through
     * {@code ArabicNormalizer.normalize()} — and the query is normalized with the
     * same function, or search silently stops matching.
     */
    @Column(name = "search_text", length = 1000)
    private String searchText;

    public void attachTo(Product parent, String locale) {
        this.product = parent;
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

        /** Populated by @MapsId — do not set it by hand. */
        @Column(name = "product_id")
        private Long productId;

        @Column(name = "locale", length = 5)
        private String locale;
    }
}
