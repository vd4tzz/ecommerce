package io.d4tzz.ecommerce.domain.catalog.category;

public record AttributeValue(String code, String label) {
    public static AttributeValue of(String code, String label) {
        return new AttributeValue(code.trim().toLowerCase(), label.trim());
    }
}
