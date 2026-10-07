package io.d4tzz.ecommerce.cart.application;

import io.d4tzz.ecommerce.cart.application.command.MergeCartCommand;
import io.d4tzz.ecommerce.cart.application.port.VariantCatalog;
import io.d4tzz.ecommerce.cart.domain.Cart;
import io.d4tzz.ecommerce.cart.domain.CartRepository;
import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import io.d4tzz.ecommerce.user.domain.UserId;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Merges a guest cart into the user's cart after login. Variants no longer for sale are skipped instead of
 * failing the whole merge, since a guest cart can be old.
 */
@Service
@Transactional
public class MergeCartUseCase {

    private final CartRepository cartRepository;
    private final VariantCatalog variantCatalog;

    public MergeCartUseCase(CartRepository cartRepository, VariantCatalog variantCatalog) {
        this.cartRepository = cartRepository;
        this.variantCatalog = variantCatalog;
    }

    public void execute(MergeCartCommand command) {
        Set<VariantId> notForSale = variantCatalog.findNotForSale(
            command.items().stream().map(item -> VariantId.of(item.variantId())).toList()
        );

        UserId userId = UserId.of(command.userId());
        cartRepository.lockForUser(userId);
        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> Cart.create(userId));

        for (MergeCartCommand.Item item : command.items()) {
            VariantId variantId = VariantId.of(item.variantId());
            if (!notForSale.contains(variantId)) {
                cart.mergeItem(variantId, item.quantity());
            }
        }

        cartRepository.save(cart);
    }
}
