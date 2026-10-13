package io.d4tzz.ecommerce.catalog.application.product;

import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.catalog.application.product.command.UpdateProductVariantCommand;
import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.catalog.domain.product.Dimensions;
import io.d4tzz.ecommerce.catalog.domain.product.MediaId;
import io.d4tzz.ecommerce.catalog.domain.product.Product;
import io.d4tzz.ecommerce.catalog.domain.product.ProductId;
import io.d4tzz.ecommerce.catalog.domain.product.ProductRepository;
import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import io.d4tzz.ecommerce.catalog.domain.product.Weight;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Updates the price, size, weight and media of a variant. */
@Service
@Transactional
public class UpdateProductVariantUseCase {

    private final ProductRepository productRepository;

    public UpdateProductVariantUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Changes the price, compare-at price, dimensions, weight and media of a variant. A blank media id removes its media,
     * and a missing compare-at price removes the compare-at price.
     *
     * @throws InvalidArgumentException if the compare-at price is lower than the price, or a size or weight is invalid
     * @throws NotFoundException if the product or the variant does not exist, or the media is not in the product's gallery
     */
    public void execute(UpdateProductVariantCommand command) {
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
        MediaId mediaId =
            command.mediaId() != null && !command.mediaId().isBlank() ? MediaId.of(command.mediaId()) : null;
        product.updateVariantMedia(vId, mediaId);

        productRepository.save(product);
    }
}
