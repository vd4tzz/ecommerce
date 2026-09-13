package io.d4tzz.ecommerce.domain.catalog.category;

import java.util.List;

import io.d4tzz.ecommerce.common.Repository;

public interface CategoryRepository extends Repository<Category, CategoryId> {
    boolean exists(CategoryId id);

    boolean existsBySlug(CategorySlug slug);

    boolean parentHasChildByName(CategoryId parentId, CategoryName name);

    boolean hasChildren(CategoryId id);

    boolean isCategoryReferenced(CategoryId id);

    void updateChildrenPrefix(String oldPrefix, String newPrefix);

    List<Attribute> findInheritedAttributes(CategoryId categoryId);

    List<Attribute> findDescendantAttributes(CategoryId categoryId);
}
