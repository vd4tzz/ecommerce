package io.d4tzz.ecommerce.application.cart;

import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.domain.cart.Cart;
import io.d4tzz.ecommerce.domain.cart.CartRepository;
import io.d4tzz.ecommerce.domain.catalog.product.VariantId;
import io.d4tzz.ecommerce.domain.user.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RemoveCartItemUseCase {

    public record Command(Long userId, Long variantId) {}

    private final CartRepository cartRepository;
    private final CartDtoAssembler cartDtoAssembler;

    public RemoveCartItemUseCase(CartRepository cartRepository, CartDtoAssembler cartDtoAssembler) {
        this.cartRepository = cartRepository;
        this.cartDtoAssembler = cartDtoAssembler;
    }

    public CartDto execute(Command command) {
        UserId userId = UserId.of(command.userId());
        Cart cart = cartRepository.findByUserId(userId)
            .orElseThrow(() -> new NotFoundException("Cart not found for user: " + command.userId()));

        cart.removeItem(VariantId.of(command.variantId()));
        Cart saved = cartRepository.save(cart);

        return cartDtoAssembler.toDto(saved);
    }
}
