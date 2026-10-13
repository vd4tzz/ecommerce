package io.d4tzz.ecommerce.catalog.application.product;

import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.catalog.application.product.command.ChangeProductStatusCommand;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.catalog.domain.product.Product;
import io.d4tzz.ecommerce.catalog.domain.product.ProductId;
import io.d4tzz.ecommerce.catalog.domain.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Publishes, archives or reverts a product's status. */
@Service
@Transactional
public class ChangeProductStatusUseCase {

    private final ProductRepository productRepository;

    public ChangeProductStatusUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Sets the status of a product. Setting the status it already has changes nothing. A product cannot go back to DRAFT.
     *
     * @throws NotFoundException if the product does not exist
     * @throws InvalidOperationException if the new status is DRAFT and the product is not already a draft
     */
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
