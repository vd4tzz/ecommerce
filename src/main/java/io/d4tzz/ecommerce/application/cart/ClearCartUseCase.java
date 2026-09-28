package io.d4tzz.ecommerce.application.cart;

import io.d4tzz.ecommerce.application.cart.command.ClearCartCommand;
import io.d4tzz.ecommerce.domain.cart.CartRepository;
import io.d4tzz.ecommerce.domain.user.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClearCartUseCase {

    private final CartRepository cartRepository;

    public ClearCartUseCase(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    public void execute(ClearCartCommand command) {
        UserId userId = UserId.of(command.userId());
        cartRepository.findByUserId(userId).ifPresent(cart -> {
            cart.clear();
            cartRepository.save(cart);
        });
    }
}
