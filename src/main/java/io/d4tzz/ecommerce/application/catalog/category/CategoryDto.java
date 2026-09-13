package io.d4tzz.ecommerce.application.catalog.category;

import io.d4tzz.ecommerce.domain.catalog.category.Category;
import java.util.List;

public record CategoryDto(
    Long id,
    String name,
    String slug,
    Long parentId,
    String path,
    List<AttributeDto> attributes
) {
    public record AttributeDto(String code, String label, String type, List<AttributeValueDto> values) {
        public record AttributeValueDto(String code, String label) {}
    }

    public static CategoryDto from(Category category) {
        return new CategoryDto(
            category.getId() != null ? category.getId().value() : null,
            category.getName().value(),
            category.getSlug().value(),
            category.getParentId() != null ? category.getParentId().value() : null,
            category.getPath(),
            category.getAttributes() != null
                ? category
                      .getAttributes()
                      .stream()
                      .map(a ->
                          new AttributeDto(
                              a.getCode(),
                              a.getLabel(),
                              a.getType().name(),
                              a.getValues() != null
                                  ? a
                                        .getValues()
                                        .stream()
                                        .map(v -> new AttributeDto.AttributeValueDto(v.code(), v.label()))
                                        .toList()
                                  : List.of()
                          )
                      )
                      .toList()
                : List.of()
        );
    }

    public static CategoryDto simple(Category category) {
        return new CategoryDto(
            category.getId() != null ? category.getId().value() : null,
            category.getName().value(),
            category.getSlug().value(),
            category.getParentId() != null ? category.getParentId().value() : null,
            category.getPath(),
            null
        );
    }
}
