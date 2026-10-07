package io.d4tzz.ecommerce.catalog.query.category;

import java.util.List;
import java.util.Optional;

public interface CategoryQueryService {
    List<CategoryTreeNode> getCategoryTree();

    List<AdminCategoryTableRowView> getAdminCategoryTreeTable();

    Optional<AdminCategoryDetailView> getAdminCategoryDetail(Long id);

    List<EffectiveAttributeView> getCategoryEffectiveAttributes(Long id);
}
