package io.d4tzz.ecommerce.application.cart;

import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.domain.cart.Cart;
import io.d4tzz.ecommerce.domain.cart.CartRepository;
import io.d4tzz.ecommerce.domain.catalog.product.VariantId;
import io.d4tzz.ecommerce.domain.user.UserId;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AddToCartUseCase {

    public record Command(Long userId, Long variantId, int quantity) {}

    private final CartRepository cartRepository;
    private final CartProductDetailProvider detailProvider;
    private final CartDtoAssembler cartDtoAssembler;

    public AddToCartUseCase(
        CartRepository cartRepository,
        CartProductDetailProvider detailProvider,
        CartDtoAssembler cartDtoAssembler
    ) {
        this.cartRepository = cartRepository;
        this.detailProvider = detailProvider;
        this.cartDtoAssembler = cartDtoAssembler;
    }

    public CartDto execute(Command command) {
        if (command.quantity() <= 0) {
            throw new InvalidArgumentException("Quantity must be greater than zero");
        }

        Map<Long, CartProductDetailProvider.VariantDetail> details = detailProvider.getVariantDetails(
            List.of(command.variantId())
        );

        CartProductDetailProvider.VariantDetail detail = details.get(command.variantId());

        if (detail == null) {
            throw new NotFoundException("Product variant not found with id: " + command.variantId());
        }
        if (!detail.active()) {
            throw new InvalidArgumentException("Product is currently not available for purchase");
        }

        UserId userId = UserId.of(command.userId());
        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> Cart.create(userId));

        cart.addItem(VariantId.of(command.variantId()), command.quantity());
        Cart saved = cartRepository.save(cart);

        return cartDtoAssembler.toDto(saved);
    }
}
