package io.d4tzz.ecommerce.catalog.application.product;

import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.catalog.domain.product.Product;
import io.d4tzz.ecommerce.catalog.domain.product.ProductId;
import io.d4tzz.ecommerce.catalog.domain.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Deletes a product that has never left DRAFT. */
@Service
@Transactional
public class DeleteProductUseCase {

    private final ProductRepository productRepository;

    public DeleteProductUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Deletes a product together with its options, media and variants.
     *
     * @throws NotFoundException if the product does not exist
     * @throws InvalidOperationException if the product is not in DRAFT status
     */
    public void execute(Long productId) {
        ProductId id = ProductId.of(productId);
        Product product = productRepository
            .find(id)
            .orElseThrow(() -> new NotFoundException("Product not found with id: " + productId));

        product.ensureCanDelete();

        productRepository.delete(id);
    }
}
