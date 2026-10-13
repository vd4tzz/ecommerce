package io.d4tzz.ecommerce.catalog.application.product;

import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.catalog.domain.option.OptionId;
import io.d4tzz.ecommerce.catalog.domain.product.Product;
import io.d4tzz.ecommerce.catalog.domain.product.ProductId;
import io.d4tzz.ecommerce.catalog.domain.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Removes an option from a product. */
@Service
@Transactional
public class RemoveProductOptionUseCase {

    private final ProductRepository productRepository;

    public RemoveProductOptionUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Removes an option from the product and from every variant's combination.
     *
     * @throws NotFoundException if the product does not exist or does not have the option
     * @throws InvalidOperationException if the option still has more than one value
     */
    public void execute(Long productId, Long optionId) {
        Product product = productRepository
            .find(ProductId.of(productId))
            .orElseThrow(() -> new NotFoundException("Product not found with id: " + productId));

        product.removeOption(OptionId.of(optionId));

        productRepository.save(product);
    }
}
