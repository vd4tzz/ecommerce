package io.d4tzz.ecommerce.catalog.application.product;

import io.d4tzz.ecommerce.catalog.application.product.command.CreateProductCommand;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.catalog.domain.category.Attribute;
import io.d4tzz.ecommerce.catalog.domain.category.Category;
import io.d4tzz.ecommerce.catalog.domain.category.CategoryId;
import io.d4tzz.ecommerce.catalog.domain.category.CategoryRepository;
import io.d4tzz.ecommerce.catalog.domain.product.Description;
import io.d4tzz.ecommerce.catalog.domain.product.Product;
import io.d4tzz.ecommerce.catalog.domain.product.ProductName;
import io.d4tzz.ecommerce.catalog.domain.product.ProductRepository;
import io.d4tzz.ecommerce.catalog.domain.product.ProductSlug;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateProductUseCase {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public CreateProductUseCase(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public Long execute(CreateProductCommand command) {
        Category category = categoryRepository
            .find(CategoryId.of(command.categoryId()))
            .orElseThrow(() -> new NotFoundException("Category not found with id: " + command.categoryId()));

        List<Attribute> schema = categoryRepository.findAttributes(category.getId());

        Product product = Product.create(
            ProductName.of(command.name()),
            ProductSlug.of(command.slug()),
            productRepository::existsBySlug,
            Description.of(command.description()),
            category.getId(),
            command.attributes(),
            schema
        );

        Product saved = productRepository.save(product);
        return saved.getId().value();
    }
}
