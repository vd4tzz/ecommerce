package io.d4tzz.ecommerce.application.catalog.product;

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
public class AddProductOptionUseCase {

    private final ProductRepository productRepository;
    private final OptionRepository optionRepository;

    public AddProductOptionUseCase(ProductRepository productRepository, OptionRepository optionRepository) {
        this.productRepository = productRepository;
        this.optionRepository = optionRepository;
    }

    public List<ProductDto.ProductOptionDto> execute(AddProductOptionCommand command) {
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

        Product saved = productRepository.save(product);
        return ProductDto.from(saved).options();
    }
}
