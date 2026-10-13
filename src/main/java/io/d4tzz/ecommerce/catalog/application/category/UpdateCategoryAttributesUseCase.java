package io.d4tzz.ecommerce.catalog.application.category;

import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.common.exception.ConflictException;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
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

/** Replaces the attribute schema of a category within the rules that keep existing products valid. */
@Service
@Transactional
public class UpdateCategoryAttributesUseCase {

    private final CategoryRepository categoryRepository;

    public UpdateCategoryAttributesUseCase(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Replaces the attributes of a category with the given list, which must include every existing attribute. New attributes
     * and new values of existing attributes can be added; nothing existing can be removed or have its type changed.
     *
     * @throws InvalidArgumentException if an attribute is invalid
     * @throws NotFoundException if the category does not exist
     * @throws ConflictException if an attribute code is duplicated, inherited from an ancestor, or already used by a descendant
     * @throws InvalidOperationException if an existing attribute or value is removed, or an attribute changes type
     */
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
