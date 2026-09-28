package io.d4tzz.ecommerce.application.catalog.category;

import io.d4tzz.ecommerce.application.catalog.category.command.UpdateCategoryCommand;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.domain.catalog.category.Category;
import io.d4tzz.ecommerce.domain.catalog.category.CategoryId;
import io.d4tzz.ecommerce.domain.catalog.category.CategoryName;
import io.d4tzz.ecommerce.domain.catalog.category.CategoryRepository;
import io.d4tzz.ecommerce.domain.catalog.category.CategorySlug;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public UpdateCategoryUseCase(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public void execute(UpdateCategoryCommand command) {
        Category category = categoryRepository
            .find(CategoryId.of(command.categoryId()))
            .orElseThrow(() -> new NotFoundException("Category not found with id: " + command.categoryId()));

        category.rename(CategoryName.of(command.name()));
        category.changeSlug(CategorySlug.of(command.slug()), categoryRepository::existsBySlug);
        categoryRepository.save(category);
    }
}
