package io.d4tzz.ecommerce.application.cart;

import io.d4tzz.ecommerce.application.cart.command.UpdateCartItemCommand;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.domain.cart.Cart;
import io.d4tzz.ecommerce.domain.cart.CartRepository;
import io.d4tzz.ecommerce.domain.catalog.product.VariantId;
import io.d4tzz.ecommerce.domain.user.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateCartItemUseCase {

    private final CartRepository cartRepository;

    public UpdateCartItemUseCase(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    public void execute(UpdateCartItemCommand command) {
        UserId userId = UserId.of(command.userId());
        Cart cart = cartRepository.findByUserId(userId)
            .orElseThrow(() -> new NotFoundException("Cart not found for user: " + command.userId()));

        cart.updateQuantity(VariantId.of(command.variantId()), command.quantity());
        cartRepository.save(cart);
    }
}
