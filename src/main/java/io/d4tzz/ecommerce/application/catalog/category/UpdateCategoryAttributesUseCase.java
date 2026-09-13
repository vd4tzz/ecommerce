package io.d4tzz.ecommerce.application.catalog.category;

import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.domain.catalog.category.Attribute;
import io.d4tzz.ecommerce.domain.catalog.category.AttributeValue;
import io.d4tzz.ecommerce.domain.catalog.category.Category;
import io.d4tzz.ecommerce.domain.catalog.category.CategoryId;
import io.d4tzz.ecommerce.domain.catalog.category.CategoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateCategoryAttributesUseCase {

    public record Response(
        Long id,
        String name,
        String slug,
        Long parentId,
        String path,
        List<AttributeResponse> attributes
    ) {
        public record AttributeResponse(String code, String label, String type, List<AttributeValueResponse> values) {
            public record AttributeValueResponse(String code, String label) {}
        }
    }

    private final CategoryRepository categoryRepository;

    public UpdateCategoryAttributesUseCase(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryDto.AttributeDto> execute(UpdateCategoryAttributesCommand command) {
        CategoryId categoryId = CategoryId.of(command.categoryId());
        Category category = categoryRepository
            .find(categoryId)
            .orElseThrow(() -> new NotFoundException("Category not found with id: " + command.categoryId()));

        List<Attribute> newAttributes = command
            .attributes()
            .stream()
            .map(cmd ->
                Attribute.create(
                    cmd.code(),
                    cmd.label(),
                    cmd.type(),
                    cmd
                        .values()
                        .stream()
                        .map(v -> AttributeValue.of(v.code(), v.label()))
                        .toList()
                )
            )
            .toList();

        List<Attribute> ancestorSchema =
            category.getParentId() != null
                ? categoryRepository.findAncestorAttributes(category.getParentId())
                : List.of();

        List<Attribute> descendantSchema = categoryRepository.findDescendantAttributes(categoryId);

        category.updateAttributes(newAttributes, ancestorSchema, descendantSchema);
        Category saved = categoryRepository.save(category);

        return CategoryDto.from(saved).attributes();
    }
}
