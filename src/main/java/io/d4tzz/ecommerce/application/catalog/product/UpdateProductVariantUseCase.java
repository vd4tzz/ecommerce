package io.d4tzz.ecommerce.application.catalog.product;

import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.domain.catalog.product.Dimensions;
import io.d4tzz.ecommerce.domain.catalog.product.MediaId;
import io.d4tzz.ecommerce.domain.catalog.product.Product;
import io.d4tzz.ecommerce.domain.catalog.product.ProductId;
import io.d4tzz.ecommerce.domain.catalog.product.ProductRepository;
import io.d4tzz.ecommerce.domain.catalog.product.VariantId;
import io.d4tzz.ecommerce.domain.catalog.product.Weight;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateProductVariantUseCase {

    private final ProductRepository productRepository;

    public UpdateProductVariantUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductDto.ProductVariantDto execute(UpdateProductVariantCommand command) {
        Product product = productRepository
            .find(ProductId.of(command.productId()))
            .orElseThrow(() -> new NotFoundException("Product not found with id: " + command.productId()));

        VariantId vId = VariantId.of(command.variantId());

        product.updateVariantPrice(
            vId,
            Money.of(command.price()),
            command.compareAt() != null ? Money.of(command.compareAt()) : null
        );

        product.updateVariantDimensions(vId, Dimensions.of(command.length(), command.width(), command.height()));
        product.updateVariantWeight(vId, Weight.ofGrams(command.weight()));
        var variant = product.updateVariantMedia(vId, MediaId.of(command.mediaId()));

        productRepository.save(product);

        return ProductDto.ProductVariantDto.from(variant);
    }
}
