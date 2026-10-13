package io.d4tzz.ecommerce.catalog.application.product;

import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.catalog.application.product.command.ReorderProductOptionsCommand;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.catalog.domain.option.OptionId;
import io.d4tzz.ecommerce.catalog.domain.product.Product;
import io.d4tzz.ecommerce.catalog.domain.product.ProductId;
import io.d4tzz.ecommerce.catalog.domain.product.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Changes the order of a product's options. */
@Service
@Transactional
public class ReorderProductOptionsUseCase {

    private final ProductRepository productRepository;

    public ReorderProductOptionsUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Sets the option order. The list must contain every option of the product exactly once.
     *
     * @throws NotFoundException if the product does not exist
     * @throws InvalidArgumentException if the list has duplicates or unknown options, or leaves out some options
     */
    public void execute(ReorderProductOptionsCommand command) {
        Product product = productRepository
            .find(ProductId.of(command.productId()))
            .orElseThrow(() -> new NotFoundException("Product not found with id: " + command.productId()));

        List<OptionId> newOrder = command.optionIds().stream().map(OptionId::of).toList();
        product.reorderOptions(newOrder);

        productRepository.save(product);
    }
}
