package io.d4tzz.ecommerce.infrastructure.catalog.persistence.entity;

import java.util.List;

public record JpaCategoryAttribute(String code, String label, String type, List<JpaAttributeValue> values) {
    public record JpaAttributeValue(String code, String label) {}
}
