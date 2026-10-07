package io.d4tzz.ecommerce.catalog.application.category;

import io.d4tzz.ecommerce.catalog.application.category.command.UpdateCategoryAttributesCommand;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.catalog.domain.category.Attribute;
import io.d4tzz.ecommerce.catalog.domain.category.AttributeValue;
import io.d4tzz.ecommerce.catalog.domain.category.Category;
import io.d4tzz.ecommerce.catalog.domain.category.CategoryId;
import io.d4tzz.ecommerce.catalog.domain.category.CategoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateCategoryAttributesUseCase {

    private final CategoryRepository categoryRepository;

    public UpdateCategoryAttributesUseCase(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public void execute(UpdateCategoryAttributesCommand command) {
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
            category.getParentId() != null ? categoryRepository.findAttributes(category.getParentId()) : List.of();

        List<Attribute> descendantSchema = categoryRepository.findDescendantAttributes(categoryId);

        category.updateAttributes(newAttributes, ancestorSchema, descendantSchema);
        categoryRepository.save(category);
    }
}
