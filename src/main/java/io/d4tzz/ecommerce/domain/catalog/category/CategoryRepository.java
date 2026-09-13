package io.d4tzz.ecommerce.domain.catalog.category;

import io.d4tzz.ecommerce.common.Repository;
import java.util.List;

public interface CategoryRepository extends Repository<Category, CategoryId> {
    boolean exists(CategoryId id);

    boolean existsBySlug(CategorySlug slug);

    boolean parentHasChildByName(CategoryId parentId, CategoryName name);

    boolean hasChildren(CategoryId id);

    boolean isCategoryReferenced(CategoryId id);

    void updateChildrenPrefix(String oldPrefix, String newPrefix);

    List<Attribute> findAncestorAttributes(CategoryId categoryId);

    List<Attribute> findDescendantAttributes(CategoryId categoryId);
}
