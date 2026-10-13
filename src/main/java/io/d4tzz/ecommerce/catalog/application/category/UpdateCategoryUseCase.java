package io.d4tzz.ecommerce.catalog.application.category;

import io.d4tzz.ecommerce.common.exception.ConflictException;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.catalog.application.category.command.UpdateCategoryCommand;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.catalog.domain.category.Category;
import io.d4tzz.ecommerce.catalog.domain.category.CategoryId;
import io.d4tzz.ecommerce.catalog.domain.category.CategoryName;
import io.d4tzz.ecommerce.catalog.domain.category.CategoryRepository;
import io.d4tzz.ecommerce.catalog.domain.category.CategorySlug;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Renames a category and changes its slug. */
@Service
@Transactional
public class UpdateCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public UpdateCategoryUseCase(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Changes the name and the slug of a category. Keeping the same slug is allowed.
     *
     * @throws InvalidArgumentException if the name or the slug is invalid
     * @throws NotFoundException if the category does not exist
     * @throws ConflictException if another category already uses the slug
     */
    public void execute(UpdateCategoryCommand command) {
        Category category = categoryRepository
            .find(CategoryId.of(command.categoryId()))
            .orElseThrow(() -> new NotFoundException("Category not found with id: " + command.categoryId()));

        category.rename(CategoryName.of(command.name()));
        category.changeSlug(CategorySlug.of(command.slug()), categoryRepository::existsBySlug);
        categoryRepository.save(category);
    }
}
