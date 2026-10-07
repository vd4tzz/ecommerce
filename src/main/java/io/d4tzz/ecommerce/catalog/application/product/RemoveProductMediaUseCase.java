package io.d4tzz.ecommerce.catalog.application.product;

import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.catalog.domain.product.MediaId;
import io.d4tzz.ecommerce.catalog.domain.product.Product;
import io.d4tzz.ecommerce.catalog.domain.product.ProductId;
import io.d4tzz.ecommerce.catalog.domain.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RemoveProductMediaUseCase {

    private final ProductRepository productRepository;

    public RemoveProductMediaUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public void execute(Long productId, String mediaId) {
        Product product = productRepository
            .find(ProductId.of(productId))
            .orElseThrow(() -> new NotFoundException("Product not found with id: " + productId));

        product.removeMedia(MediaId.of(mediaId));

        productRepository.save(product);
    }
}
