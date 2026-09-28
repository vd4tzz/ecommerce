package io.d4tzz.ecommerce.application.catalog.product;

import io.d4tzz.ecommerce.application.catalog.product.command.ReorderProductMediaCommand;
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
public class ReorderProductMediaUseCase {

    private final ProductRepository productRepository;

    public ReorderProductMediaUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public void execute(ReorderProductMediaCommand command) {
        Product product = productRepository
            .find(ProductId.of(command.productId()))
            .orElseThrow(() -> new NotFoundException("Product not found with id: " + command.productId()));

        List<MediaId> newOrder = command.mediaIds().stream().map(MediaId::of).toList();
        product.reorderMedia(newOrder);

        productRepository.save(product);
    }
}
