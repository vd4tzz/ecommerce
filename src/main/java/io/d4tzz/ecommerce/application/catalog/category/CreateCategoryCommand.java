package io.d4tzz.ecommerce.application.catalog.category;

import io.d4tzz.ecommerce.domain.catalog.category.AttributeType;
import java.util.List;

public record CreateCategoryCommand(String name, String slug, Long parentId, List<Attribute> attributes) {
    public CreateCategoryCommand {
        attributes = attributes == null ? List.of() : attributes;
    }

    public record Attribute(String code, String label, AttributeType type, List<Value> values) {
        public Attribute {
            values = values == null ? List.of() : values;
        }
    }

    public record Value(String code, String label) {}
}
