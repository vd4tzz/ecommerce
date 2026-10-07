package io.d4tzz.ecommerce.catalog.application.category;

import io.d4tzz.ecommerce.catalog.application.category.command.CreateCategoryCommand;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.catalog.domain.category.Attribute;
import io.d4tzz.ecommerce.catalog.domain.category.AttributeValue;
import io.d4tzz.ecommerce.catalog.domain.category.Category;
import io.d4tzz.ecommerce.catalog.domain.category.CategoryId;
import io.d4tzz.ecommerce.catalog.domain.category.CategoryName;
import io.d4tzz.ecommerce.catalog.domain.category.CategoryRepository;
import io.d4tzz.ecommerce.catalog.domain.category.CategorySlug;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public CreateCategoryUseCase(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public Long execute(CreateCategoryCommand command) {
        Category parent = null;
        List<Attribute> ancestorSchema = List.of();

        if (command.parentId() != null) {
            CategoryId parentCategoryId = CategoryId.of(command.parentId());
            parent = categoryRepository
                .find(parentCategoryId)
                .orElseThrow(() -> new NotFoundException("Parent category not found with id: " + command.parentId()));
            ancestorSchema = categoryRepository.findAttributes(parentCategoryId);
        }

        List<Attribute> attributes = command
            .attributes()
            .stream()
            .map(attr ->
                Attribute.create(
                    attr.code(),
                    attr.label(),
                    attr.type(),
                    attr
                        .values()
                        .stream()
                        .map(v -> AttributeValue.of(v.code(), v.label()))
                        .toList()
                )
            )
            .toList();

        Category category = Category.create(
            CategoryName.of(command.name()),
            CategorySlug.of(command.slug()),
            categoryRepository::existsBySlug,
            parent,
            attributes,
            ancestorSchema
        );

        Category saved = categoryRepository.save(category);

        return saved.getId().value();
    }
}
