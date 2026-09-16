package io.d4tzz.ecommerce.application.catalog.product;

import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.domain.catalog.product.MediaId;
import io.d4tzz.ecommerce.domain.catalog.product.Product;
import io.d4tzz.ecommerce.domain.catalog.product.ProductId;
import io.d4tzz.ecommerce.domain.catalog.product.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AddProductMediaUseCase {

    private final ProductRepository productRepository;

    public AddProductMediaUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<String> execute(AddProductMediaCommand command) {
        Product product = productRepository
            .find(ProductId.of(command.productId()))
            .orElseThrow(() -> new NotFoundException("Product not found with id: " + command.productId()));

        for (String mediaId : command.mediaIds()) {
            product.addMedia(MediaId.of(mediaId));
        }

        Product saved = productRepository.save(product);
        return ProductDto.from(saved).media();
    }
}
