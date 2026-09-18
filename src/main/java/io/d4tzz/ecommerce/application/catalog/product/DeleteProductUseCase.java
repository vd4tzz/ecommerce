package io.d4tzz.ecommerce.application.catalog.product;

import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.domain.catalog.product.Product;
import io.d4tzz.ecommerce.domain.catalog.product.ProductId;
import io.d4tzz.ecommerce.domain.catalog.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeleteProductUseCase {

    private final ProductRepository productRepository;

    public DeleteProductUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public void execute(Long productId) {
        ProductId id = ProductId.of(productId);
        Product product = productRepository
            .find(id)
            .orElseThrow(() -> new NotFoundException("Product not found with id: " + productId));

        product.ensureCanDelete();

        productRepository.delete(id);
    }
}
