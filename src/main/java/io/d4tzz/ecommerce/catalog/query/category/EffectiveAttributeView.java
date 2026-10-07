package io.d4tzz.ecommerce.catalog.query.category;

import java.util.List;

public record EffectiveAttributeView(
    String code,
    String label,
    String type,
    List<AttributeValueView> values,
    Long categoryId,
    String categoryName,
    boolean isInherited
) {
    public record AttributeValueView(String code, String label) {}
}
