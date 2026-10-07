package io.d4tzz.ecommerce.catalog.application.category;

import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.catalog.domain.category.CategoryId;
import io.d4tzz.ecommerce.catalog.domain.category.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeleteCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public DeleteCategoryUseCase(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public void execute(Long categoryId) {
        CategoryId id = CategoryId.of(categoryId);
        if (!categoryRepository.exists(id)) {
            throw new NotFoundException(
                "Category not found with id: " + categoryId
            );
        }

        if (categoryRepository.hasChildren(id)) {
            throw new InvalidOperationException(
                "Cannot delete category because it has sub-categories"
            );
        }

        if (categoryRepository.isCategoryReferenced(id)) {
            throw new InvalidOperationException(
                "Cannot delete category because it is currently assigned to products"
            );
        }

        categoryRepository.delete(id);
    }
}
