package io.d4tzz.ecommerce.application.catalog.product;

import io.d4tzz.ecommerce.application.catalog.product.command.ReorderProductOptionValuesCommand;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.domain.catalog.option.OptionId;
import io.d4tzz.ecommerce.domain.catalog.option.ValueId;
import io.d4tzz.ecommerce.domain.catalog.product.Product;
import io.d4tzz.ecommerce.domain.catalog.product.ProductId;
import io.d4tzz.ecommerce.domain.catalog.product.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReorderProductOptionValuesUseCase {

    private final ProductRepository productRepository;

    public ReorderProductOptionValuesUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public void execute(ReorderProductOptionValuesCommand command) {
        Product product = productRepository
            .find(ProductId.of(command.productId()))
            .orElseThrow(() -> new NotFoundException("Product not found with id: " + command.productId()));

        List<ValueId> newOrder = command.valueIds().stream().map(ValueId::of).toList();
        product.reorderOptionValues(OptionId.of(command.optionId()), newOrder);

        productRepository.save(product);
    }
}
