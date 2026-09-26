package io.d4tzz.ecommerce.application.cart;

import io.d4tzz.ecommerce.domain.cart.Cart;
import io.d4tzz.ecommerce.domain.cart.CartRepository;
import io.d4tzz.ecommerce.domain.catalog.product.VariantId;
import io.d4tzz.ecommerce.domain.user.UserId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MergeCartUseCase {

    public record Item(Long variantId, int quantity) {}

    public record Command(Long userId, List<Item> items) {}

    private final CartRepository cartRepository;

    public MergeCartUseCase(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    public void execute(Command command) {
        UserId userId = UserId.of(command.userId());
        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> Cart.create(userId));

        for (Item item : command.items()) {
            cart.mergeItem(VariantId.of(item.variantId()), item.quantity());
        }

        cartRepository.save(cart);
    }
}
