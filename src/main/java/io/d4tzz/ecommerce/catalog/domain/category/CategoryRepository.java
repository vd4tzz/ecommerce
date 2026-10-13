package io.d4tzz.ecommerce.catalog.domain.category;

import io.d4tzz.ecommerce.common.Repository;
import java.util.List;

/**
 * Persistence port for {@link Category}.
 *
 * <p>Besides the basic {@code find}, {@code save} and {@code delete}, it exposes the existence checks
 * and attribute-schema lookups the category use cases need.
 */
public interface CategoryRepository extends Repository<Category, CategoryId> {
    /** Tells whether a category with the given id exists. */
    boolean exists(CategoryId id);

    /** Tells whether any category already uses the given slug. */
    boolean existsBySlug(CategorySlug slug);

    /**
     * Tells whether the given parent already has a direct child with this name.
     *
     * @param parentId the parent category id, {@code null} to check among root categories
     */
    boolean parentHasChildByName(CategoryId parentId, CategoryName name);

    /** Tells whether the category has at least one direct child. */
    boolean hasChildren(CategoryId id);

    /** Tells whether any product is assigned to the category. */
    boolean isCategoryReferenced(CategoryId id);

    /**
     * Rewrites the path of every category whose path starts with {@code oldPrefix}, replacing
     * {@code oldPrefix} with {@code newPrefix}. Used to update descendants after a category is moved.
     *
     * @param oldPrefix the descendants' current path prefix, as returned by {@link Category#prefix()}
     * @param newPrefix the path prefix they should have after the move
     */
    void updateChildrenPrefix(String oldPrefix, String newPrefix);

    /**
     * Returns the full attribute schema of a category: the attributes of all its ancestors
     * followed by its own.
     *
     * @return the attributes, or an empty list if the category does not exist
     */
    List<Attribute> findAttributes(CategoryId categoryId);

    /**
     * Returns the attributes defined by all descendants of a category, at any depth.
     * The category's own attributes are not included.
     *
     * @return the attributes, or an empty list if the category does not exist or has no descendants
     */
    List<Attribute> findDescendantAttributes(CategoryId categoryId);
}
