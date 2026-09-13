package io.d4tzz.ecommerce.query.catalog;

public record AdminCategoryTableRowView(
    Long id,
    String name,
    String slug,
    String path,
    Long parentId,
    int level,
    boolean hasChildren,
    long productCount,
    int attributeCount
) {}
