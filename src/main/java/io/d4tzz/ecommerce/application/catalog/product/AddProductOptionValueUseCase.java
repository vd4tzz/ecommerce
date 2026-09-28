package io.d4tzz.ecommerce.application.catalog.product;

import io.d4tzz.ecommerce.application.catalog.product.command.AddProductOptionValueCommand;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.domain.catalog.option.OptionId;
import io.d4tzz.ecommerce.domain.catalog.option.OptionRepository;
import io.d4tzz.ecommerce.domain.catalog.option.ValueId;
import io.d4tzz.ecommerce.domain.catalog.product.Product;
import io.d4tzz.ecommerce.domain.catalog.product.ProductId;
import io.d4tzz.ecommerce.domain.catalog.product.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AddProductOptionValueUseCase {

    private final ProductRepository productRepository;
    private final OptionRepository optionRepository;

    public AddProductOptionValueUseCase(ProductRepository productRepository, OptionRepository optionRepository) {
        this.productRepository = productRepository;
        this.optionRepository = optionRepository;
    }

    public void execute(AddProductOptionValueCommand command) {
        Product product = productRepository
            .find(ProductId.of(command.productId()))
            .orElseThrow(() -> new NotFoundException("Product not found with id: " + command.productId()));

        OptionId optId = OptionId.of(command.optionId());
        List<ValueId> valIds = command.valueIds().stream().map(ValueId::of).toList();

        if (!optionRepository.areValuesBelongToOption(optId, valIds)) {
            throw new InvalidArgumentException("Some values do not belong to option: " + command.optionId());
        }

        for (ValueId valId : valIds) {
            product.addValue(optId, valId);
        }

        productRepository.save(product);
    }
}
