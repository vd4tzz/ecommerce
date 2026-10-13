package io.d4tzz.ecommerce.catalog.application.product;

import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.catalog.application.product.command.ReorderProductOptionValuesCommand;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.catalog.domain.option.OptionId;
import io.d4tzz.ecommerce.catalog.domain.option.ValueId;
import io.d4tzz.ecommerce.catalog.domain.product.Product;
import io.d4tzz.ecommerce.catalog.domain.product.ProductId;
import io.d4tzz.ecommerce.catalog.domain.product.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Changes the order of the values of one option of a product. */
@Service
@Transactional
public class ReorderProductOptionValuesUseCase {

    private final ProductRepository productRepository;

    public ReorderProductOptionValuesUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Sets the order of the option's values. The list must contain every value of the option on the product exactly once.
     *
     * @throws NotFoundException if the product does not exist or does not have the option
     * @throws InvalidArgumentException if the list has duplicates or unknown values, or leaves out some values
     */
    public void execute(ReorderProductOptionValuesCommand command) {
        Product product = productRepository
            .find(ProductId.of(command.productId()))
            .orElseThrow(() -> new NotFoundException("Product not found with id: " + command.productId()));

        List<ValueId> newOrder = command.valueIds().stream().map(ValueId::of).toList();
        product.reorderOptionValues(OptionId.of(command.optionId()), newOrder);

        productRepository.save(product);
    }
}
