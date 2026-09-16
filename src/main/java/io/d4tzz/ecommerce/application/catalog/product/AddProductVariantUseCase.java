package io.d4tzz.ecommerce.application.catalog.product;

import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.domain.catalog.option.OptionId;
import io.d4tzz.ecommerce.domain.catalog.option.ValueId;
import io.d4tzz.ecommerce.domain.catalog.product.Dimensions;
import io.d4tzz.ecommerce.domain.catalog.product.MediaId;
import io.d4tzz.ecommerce.domain.catalog.product.Product;
import io.d4tzz.ecommerce.domain.catalog.product.ProductId;
import io.d4tzz.ecommerce.domain.catalog.product.ProductRepository;
import io.d4tzz.ecommerce.domain.catalog.product.Sku;
import io.d4tzz.ecommerce.domain.catalog.product.Variant;
import io.d4tzz.ecommerce.domain.catalog.product.Weight;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    public List<ProductDto.ProductVariantDto> execute(AddProductVariantCommand command) {
        Product product = productRepository
            .find(ProductId.of(command.productId()))
            .orElseThrow(() -> new NotFoundException("Product not found with id: " + command.productId()));

        List<Variant> variants = new ArrayList<>(command.variants().size());
        for (var variantItem : command.variants()) {
            Map<OptionId, ValueId> combination = HashMap.newHashMap(variantItem.combination().size());
            variantItem.combination().forEach((optId, valId) -> combination.put(OptionId.of(optId), ValueId.of(valId)));

            var variant = product.addVariant(
                Sku.of(variantItem.sku()),
                Money.of(variantItem.price()),
                MediaId.of(variantItem.mediaId()),
                Dimensions.of(variantItem.length(), variantItem.width(), variantItem.height()),
                Weight.ofGrams(variantItem.weight()),
                combination,
                productRepository::existsBySku
            );
            variants.add(variant);
        }

        return variants
            .stream()
            .map(v -> ProductDto.ProductVariantDto.from(v))
            .toList();
    }
}
