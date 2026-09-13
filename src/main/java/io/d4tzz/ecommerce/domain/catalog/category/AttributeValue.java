package io.d4tzz.ecommerce.domain.catalog.category;

public record AttributeValue(String code, String label) {
    public AttributeValue {
        if (code.isBlank()) {
            throw new IllegalArgumentException("Attribute value code cannot be blank");
        }
        if (label.isBlank()) {
            throw new IllegalArgumentException("Attribute value label cannot be blank");
        }
    }

    public static AttributeValue of(String code, String label) {
        return new AttributeValue(code.trim().toLowerCase(), label.trim());
    }
}
