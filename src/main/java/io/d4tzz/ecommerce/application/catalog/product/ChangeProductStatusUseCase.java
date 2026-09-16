package io.d4tzz.ecommerce.application.catalog.product;

import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.domain.catalog.product.Product;
import io.d4tzz.ecommerce.domain.catalog.product.ProductId;
import io.d4tzz.ecommerce.domain.catalog.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ChangeProductStatusUseCase {

    private final ProductRepository productRepository;

    public ChangeProductStatusUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public void execute(ChangeProductStatusCommand command) {
        Product product = productRepository
            .find(ProductId.of(command.productId()))
            .orElseThrow(() -> new NotFoundException("Product not found with id: " + command.productId()));

        switch (command.status()) {
            case PUBLISHED -> product.publish();
            case ARCHIVED -> product.archive();
            case DRAFT -> product.draft();
        }

        productRepository.save(product);
    }
}
