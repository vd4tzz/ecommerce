package io.d4tzz.ecommerce.application.catalog.product;

import io.d4tzz.ecommerce.application.catalog.product.command.UpdateProductCommand;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.domain.catalog.category.Attribute;
import io.d4tzz.ecommerce.domain.catalog.category.Category;
import io.d4tzz.ecommerce.domain.catalog.category.CategoryId;
import io.d4tzz.ecommerce.domain.catalog.category.CategoryRepository;
import io.d4tzz.ecommerce.domain.catalog.product.Description;
import io.d4tzz.ecommerce.domain.catalog.product.Product;
import io.d4tzz.ecommerce.domain.catalog.product.ProductId;
import io.d4tzz.ecommerce.domain.catalog.product.ProductName;
import io.d4tzz.ecommerce.domain.catalog.product.ProductRepository;
import io.d4tzz.ecommerce.domain.catalog.product.ProductSlug;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateProductUseCase {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public UpdateProductUseCase(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public void execute(UpdateProductCommand command) {
        Product product = productRepository
            .find(ProductId.of(command.productId()))
            .orElseThrow(() -> new NotFoundException("Product not found with id: " + command.productId()));

        product.rename(ProductName.of(command.name()));
        product.changeSlug(ProductSlug.of(command.slug()), productRepository::existsBySlug);
        product.changeDescription(Description.of(command.description()));

        Category category = categoryRepository
            .find(CategoryId.of(command.categoryId()))
            .orElseThrow(() -> new NotFoundException("Category not found with id: " + command.categoryId()));
        List<Attribute> schema = categoryRepository.findAttributes(category.getId());

        if (product.getCategoryId() != category.getId()) {
            product.changeCategory(category.getId(), command.attributes(), schema);
        } else {
            product.updateAttributes(command.attributes(), schema);
        }

        productRepository.save(product);
    }
}
