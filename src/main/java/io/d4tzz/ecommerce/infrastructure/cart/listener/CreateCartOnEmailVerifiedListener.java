package io.d4tzz.ecommerce.infrastructure.cart.listener;

import io.d4tzz.ecommerce.domain.cart.Cart;
import io.d4tzz.ecommerce.domain.cart.CartRepository;
import io.d4tzz.ecommerce.domain.user.event.EmailVerifiedEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class CreateCartOnEmailVerifiedListener {

    private final CartRepository cartRepository;

    public CreateCartOnEmailVerifiedListener(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    @ApplicationModuleListener
    public void onEmailVerified(EmailVerifiedEvent event) {
        if (cartRepository.findByUserId(event.userId()).isEmpty()) {
            cartRepository.save(Cart.create(event.userId()));
        }
    }
}
