package io.d4tzz.ecommerce.cart.application;

import io.d4tzz.ecommerce.cart.application.command.RemoveCartItemCommand;
import io.d4tzz.ecommerce.cart.domain.CartRepository;
import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import io.d4tzz.ecommerce.user.domain.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Removes a variant from the user's cart. Nothing happens when the cart or the item does not exist. */
@Service
@Transactional
public class RemoveCartItemUseCase {

    private final CartRepository cartRepository;

    public RemoveCartItemUseCase(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    public void execute(RemoveCartItemCommand command) {
        UserId userId = UserId.of(command.userId());
        cartRepository.lockForUser(userId);
        cartRepository.findByUserId(userId).ifPresent(cart -> {
            cart.removeItem(VariantId.of(command.variantId()));
            cartRepository.save(cart);
        });
    }
}
