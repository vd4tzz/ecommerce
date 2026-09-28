package io.d4tzz.ecommerce.query.catalog.category;

import java.util.List;

public record AdminCategoryDetailView(
    Long id,
    String name,
    String slug,
    Long parentId,
    String path,
    boolean isDeletable,
    List<AttributeConfigView> attributes
) {
    public record AttributeConfigView(
        String code,
        String label,
        String type,
        List<AttributeValueView> values
    ) {
        public record AttributeValueView(String code, String label) {}
    }
}
