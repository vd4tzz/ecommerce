package io.d4tzz.ecommerce.catalog.application.product;

import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.catalog.domain.option.OptionId;
import io.d4tzz.ecommerce.catalog.domain.option.ValueId;
import io.d4tzz.ecommerce.catalog.domain.product.Product;
import io.d4tzz.ecommerce.catalog.domain.product.ProductId;
import io.d4tzz.ecommerce.catalog.domain.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RemoveProductOptionValueUseCase {

    private final ProductRepository productRepository;

    public RemoveProductOptionValueUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public void execute(Long productId, Long optionId, Long valueId) {
        Product product = productRepository
            .find(ProductId.of(productId))
            .orElseThrow(() -> new NotFoundException("Product not found with id: " + productId));

        product.removeValue(OptionId.of(optionId), ValueId.of(valueId));

        productRepository.save(product);
    }
}
