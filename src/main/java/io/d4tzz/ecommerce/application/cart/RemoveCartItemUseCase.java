package io.d4tzz.ecommerce.application.cart;

import io.d4tzz.ecommerce.application.cart.command.RemoveCartItemCommand;
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

    private final CartRepository cartRepository;

    public RemoveCartItemUseCase(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    public void execute(RemoveCartItemCommand command) {
        UserId userId = UserId.of(command.userId());
        Cart cart = cartRepository.findByUserId(userId)
            .orElseThrow(() -> new NotFoundException("Cart not found for user: " + command.userId()));

        cart.removeItem(VariantId.of(command.variantId()));
        cartRepository.save(cart);
    }
}
