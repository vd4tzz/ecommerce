package io.d4tzz.ecommerce.catalog.application.product;

import io.d4tzz.ecommerce.common.exception.ConflictException;
import io.d4tzz.ecommerce.catalog.application.product.command.AddProductOptionCommand;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.catalog.domain.option.OptionId;
import io.d4tzz.ecommerce.catalog.domain.option.OptionRepository;
import io.d4tzz.ecommerce.catalog.domain.option.ValueId;
import io.d4tzz.ecommerce.catalog.domain.product.Product;
import io.d4tzz.ecommerce.catalog.domain.product.ProductId;
import io.d4tzz.ecommerce.catalog.domain.product.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Adds an option to a product, with the values the product offers for it. */
@Service
@Transactional
public class AddProductOptionUseCase {

    private final ProductRepository productRepository;
    private final OptionRepository optionRepository;

    public AddProductOptionUseCase(ProductRepository productRepository, OptionRepository optionRepository) {
        this.productRepository = productRepository;
        this.optionRepository = optionRepository;
    }

    /**
     * Adds an option with the given values, which must belong to the option. The first value becomes the value of every
     * existing variant for this option.
     *
     * @throws NotFoundException if the product or the option does not exist
     * @throws InvalidArgumentException if some values do not belong to the option
     * @throws ConflictException if the product already has the option
     */
    public void execute(AddProductOptionCommand command) {
        Product product = productRepository
            .find(ProductId.of(command.productId()))
            .orElseThrow(() -> new NotFoundException("Product not found with id: " + command.productId()));

        OptionId optId = OptionId.of(command.optionId());
        List<ValueId> valIds = command.valueIds().stream().map(ValueId::of).toList();

        if (!optionRepository.existsById(optId)) {
            throw new NotFoundException("Option not found with id: " + command.optionId());
        }

        if (!optionRepository.areValuesBelongToOption(optId, valIds)) {
            throw new InvalidArgumentException("Some values do not belong to option: " + command.optionId());
        }

        ValueId defaultValueId = valIds.get(0);
        product.addOption(optId, defaultValueId);

        for (int i = 1; i < valIds.size(); i++) {
            product.addValue(optId, valIds.get(i));
        }

        productRepository.save(product);
    }
}
