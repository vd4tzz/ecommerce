package io.d4tzz.ecommerce.infrastructure.catalog.category;

import java.util.List;

public record JpaCategoryAttribute(
    String code,
    String label,
    String type,
    List<JpaAttributeValue> values
) {
    public JpaCategoryAttribute {
        values = values != null ? values : List.of();
    }

    public record JpaAttributeValue(String code, String label) {}
}
