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
 * Translated content for one category in one locale.
 *
 * <p>The {@code @MapsId} association is what fills {@code category_id}. Mapping the
 * parent with {@code mappedBy} pointing at a plain id FIELD does not work: Hibernate
 * never populates the foreign key and the insert fails with a NULL constraint
 * violation. The owning side must be a real association.
 */
@Entity
@Table(name = "category_translation")
@Getter
@Setter
@NoArgsConstructor
public class CategoryTranslation {

    @EmbeddedId
    private Key key = new Key();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("categoryId")
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "meta_title", length = 255)
    private String metaTitle;

    @Column(name = "meta_description", length = 500)
    private String metaDescription;

    /** Convenience: sets the parent and the locale half of the key together. */
    public void attachTo(Category parent, String locale) {
        this.category = parent;
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
        @Column(name = "category_id")
        private Long categoryId;

        @Column(name = "locale", length = 5)
        private String locale;
    }
}
