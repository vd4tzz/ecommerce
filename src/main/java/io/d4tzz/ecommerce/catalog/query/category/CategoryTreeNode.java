package io.d4tzz.ecommerce.catalog.query.category;

import java.util.List;

public record CategoryTreeNode(
    Long id,
    String name,
    String slug,
    String path,
    Long parentId,
    List<CategoryTreeNode> children
) {}
