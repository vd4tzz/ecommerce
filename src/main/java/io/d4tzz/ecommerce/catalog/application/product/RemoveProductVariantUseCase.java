package io.d4tzz.ecommerce.catalog.application.product;

import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.catalog.domain.product.Product;
import io.d4tzz.ecommerce.catalog.domain.product.ProductId;
import io.d4tzz.ecommerce.catalog.domain.product.ProductRepository;
import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Removes a variant from a draft product. */
@Service
@Transactional
public class RemoveProductVariantUseCase {

    private final ProductRepository productRepository;

    public RemoveProductVariantUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Removes the variant. Nothing happens if the product has no variant with that id.
     *
     * @throws NotFoundException if the product does not exist
     * @throws InvalidOperationException if the product is not in DRAFT status
     */
    public void execute(Long productId, Long variantId) {
        Product product = productRepository
            .find(ProductId.of(productId))
            .orElseThrow(() -> new NotFoundException("Product not found with id: " + productId));

        product.removeVariant(VariantId.of(variantId));

        productRepository.save(product);
    }
}
