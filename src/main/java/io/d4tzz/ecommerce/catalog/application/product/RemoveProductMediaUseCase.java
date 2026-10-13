package io.d4tzz.ecommerce.catalog.application.product;

import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.catalog.domain.product.MediaId;
import io.d4tzz.ecommerce.catalog.domain.product.Product;
import io.d4tzz.ecommerce.catalog.domain.product.ProductId;
import io.d4tzz.ecommerce.catalog.domain.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Removes media from a product's gallery. */
@Service
@Transactional
public class RemoveProductMediaUseCase {

    private final ProductRepository productRepository;

    public RemoveProductMediaUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Removes the media from the gallery. Nothing happens if it is not in the gallery.
     *
     * @throws NotFoundException if the product does not exist
     * @throws InvalidArgumentException if a variant uses the media
     */
    public void execute(Long productId, String mediaId) {
        Product product = productRepository
            .find(ProductId.of(productId))
            .orElseThrow(() -> new NotFoundException("Product not found with id: " + productId));

        product.removeMedia(MediaId.of(mediaId));

        productRepository.save(product);
    }
}
