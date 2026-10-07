package io.d4tzz.ecommerce.cart.application;

import io.d4tzz.ecommerce.cart.application.command.ClearCartCommand;
import io.d4tzz.ecommerce.cart.domain.CartRepository;
import io.d4tzz.ecommerce.domain.user.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Empties the user's cart. Nothing happens when the user has no cart. */
@Service
@Transactional
public class ClearCartUseCase {

    private final CartRepository cartRepository;

    public ClearCartUseCase(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    public void execute(ClearCartCommand command) {
        UserId userId = UserId.of(command.userId());
        cartRepository.lockForUser(userId);
        cartRepository.findByUserId(userId).ifPresent(cart -> {
            cart.clear();
            cartRepository.save(cart);
        });
    }
}
