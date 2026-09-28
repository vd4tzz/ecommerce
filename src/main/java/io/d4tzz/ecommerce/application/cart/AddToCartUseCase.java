package io.d4tzz.ecommerce.application.cart;

import io.d4tzz.ecommerce.application.cart.command.AddToCartCommand;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.domain.cart.Cart;
import io.d4tzz.ecommerce.domain.cart.CartRepository;
import io.d4tzz.ecommerce.domain.catalog.product.VariantId;
import io.d4tzz.ecommerce.domain.user.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AddToCartUseCase {

    private final CartRepository cartRepository;

    public AddToCartUseCase(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    public void execute(AddToCartCommand command) {
        if (command.quantity() <= 0) {
            throw new InvalidArgumentException("Quantity must be greater than zero");
        }

        UserId userId = UserId.of(command.userId());
        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> Cart.create(userId));

        cart.addItem(VariantId.of(command.variantId()), command.quantity());
        cartRepository.save(cart);
    }
}
