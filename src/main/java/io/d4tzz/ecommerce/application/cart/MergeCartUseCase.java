package io.d4tzz.ecommerce.application.cart;

import io.d4tzz.ecommerce.application.cart.command.MergeCartCommand;
import io.d4tzz.ecommerce.domain.cart.Cart;
import io.d4tzz.ecommerce.domain.cart.CartRepository;
import io.d4tzz.ecommerce.domain.catalog.product.VariantId;
import io.d4tzz.ecommerce.domain.user.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MergeCartUseCase {

    private final CartRepository cartRepository;

    public MergeCartUseCase(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    public void execute(MergeCartCommand command) {
        UserId userId = UserId.of(command.userId());
        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> Cart.create(userId));

        for (MergeCartCommand.Item item : command.items()) {
            cart.mergeItem(VariantId.of(item.variantId()), item.quantity());
        }

        cartRepository.save(cart);
    }
}
