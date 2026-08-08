package com.velora.api.catalog.repository;

import com.velora.api.catalog.domain.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @EntityGraph(attributePaths = {"translations"})
    Optional<Category> findBySlugAndActiveTrue(String slug);

    /**
     * The whole tree in one query. The header mega-menu needs it on every page, so
     * fetching level by level would be several round trips for data that changes
     * once a month — cache the result.
     */
    @EntityGraph(attributePaths = {"translations"})
    List<Category> findByActiveTrueOrderByDisplayOrderAscIdAsc();

    List<Category> findByParentIdAndActiveTrueOrderByDisplayOrderAsc(Long parentId);

    boolean existsBySlug(String slug);
}
