package io.d4tzz.ecommerce.cart.inbound.listener;

import io.d4tzz.ecommerce.cart.domain.Cart;
import io.d4tzz.ecommerce.cart.domain.CartRepository;
import io.d4tzz.ecommerce.user.domain.event.EmailVerifiedEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Gives a user an empty cart as soon as their email is verified. */
@Component
public class CreateCartOnEmailVerifiedListener {

    private final CartRepository cartRepository;

    public CreateCartOnEmailVerifiedListener(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    /** Creates the user's cart if they do not have one yet, so a repeated event creates no second cart. */
    @ApplicationModuleListener
    public void onEmailVerified(EmailVerifiedEvent event) {
        cartRepository.lockForUser(event.userId());
        if (cartRepository.findByUserId(event.userId()).isEmpty()) {
            cartRepository.save(Cart.create(event.userId()));
        }
    }
}
