package io.d4tzz.ecommerce.application.catalog.product;

import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.domain.catalog.option.OptionId;
import io.d4tzz.ecommerce.domain.catalog.product.Product;
import io.d4tzz.ecommerce.domain.catalog.product.ProductId;
import io.d4tzz.ecommerce.domain.catalog.product.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReorderProductOptionsUseCase {

    private final ProductRepository productRepository;

    public ReorderProductOptionsUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductDto.ProductOptionDto> execute(ReorderProductOptionsCommand command) {
        Product product = productRepository
            .find(ProductId.of(command.productId()))
            .orElseThrow(() -> new NotFoundException("Product not found with id: " + command.productId()));

        List<OptionId> newOrder = command.optionIds().stream().map(OptionId::of).toList();
        product.reorderOptions(newOrder);

        Product saved = productRepository.save(product);
        return ProductDto.from(saved).options();
    }
}
