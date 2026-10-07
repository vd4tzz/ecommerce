package io.d4tzz.ecommerce.catalog.application.product;

import io.d4tzz.ecommerce.catalog.application.product.command.AddProductVariantCommand;
import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.catalog.domain.option.OptionId;
import io.d4tzz.ecommerce.catalog.domain.option.ValueId;
import io.d4tzz.ecommerce.catalog.domain.product.Dimensions;
import io.d4tzz.ecommerce.catalog.domain.product.MediaId;
import io.d4tzz.ecommerce.catalog.domain.product.Product;
import io.d4tzz.ecommerce.catalog.domain.product.ProductId;
import io.d4tzz.ecommerce.catalog.domain.product.ProductRepository;
import io.d4tzz.ecommerce.catalog.domain.product.Sku;
import io.d4tzz.ecommerce.catalog.domain.product.Weight;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AddProductVariantUseCase {

    private final ProductRepository productRepository;

    public AddProductVariantUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public void execute(AddProductVariantCommand command) {
        Product product = productRepository
            .find(ProductId.of(command.productId()))
            .orElseThrow(() -> new NotFoundException("Product not found with id: " + command.productId()));

        for (var variantItem : command.variants()) {
            Map<OptionId, ValueId> combination = HashMap.newHashMap(variantItem.combination().size());
            variantItem.combination().forEach((optId, valId) -> combination.put(OptionId.of(optId), ValueId.of(valId)));

            MediaId mediaId =
                variantItem.mediaId() != null && !variantItem.mediaId().isBlank()
                    ? MediaId.of(variantItem.mediaId())
                    : null;

            product.addVariant(
                Sku.of(variantItem.sku()),
                Money.of(variantItem.price()),
                mediaId,
                Dimensions.of(variantItem.length(), variantItem.width(), variantItem.height()),
                Weight.ofGrams(variantItem.weight()),
                combination,
                productRepository::existsBySku
            );
        }

        productRepository.save(product);
    }
}
