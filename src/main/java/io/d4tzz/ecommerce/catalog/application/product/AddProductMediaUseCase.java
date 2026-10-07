package io.d4tzz.ecommerce.catalog.application.product;

import io.d4tzz.ecommerce.catalog.application.product.command.AddProductMediaCommand;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.catalog.domain.product.MediaId;
import io.d4tzz.ecommerce.catalog.domain.product.Product;
import io.d4tzz.ecommerce.catalog.domain.product.ProductId;
import io.d4tzz.ecommerce.catalog.domain.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AddProductMediaUseCase {

    private final ProductRepository productRepository;

    public AddProductMediaUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public void execute(AddProductMediaCommand command) {
        Product product = productRepository
            .find(ProductId.of(command.productId()))
            .orElseThrow(() -> new NotFoundException("Product not found with id: " + command.productId()));

        for (String mediaId : command.mediaIds()) {
            product.addMedia(MediaId.of(mediaId));
        }

        productRepository.save(product);
    }
}
