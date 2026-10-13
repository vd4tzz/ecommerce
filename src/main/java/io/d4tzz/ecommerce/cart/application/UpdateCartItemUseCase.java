package io.d4tzz.ecommerce.cart.application;

import io.d4tzz.ecommerce.cart.domain.CartItem;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.cart.application.command.UpdateCartItemCommand;
import io.d4tzz.ecommerce.cart.domain.Cart;
import io.d4tzz.ecommerce.cart.domain.CartRepository;
import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import io.d4tzz.ecommerce.catalog.published.VariantSaleChecker;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.user.domain.UserId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sets the quantity of a variant in the user's cart, adding it (and the cart) when missing. */
@Service
@Transactional
public class UpdateCartItemUseCase {

    private final CartRepository cartRepository;
    private final VariantSaleChecker variantSaleChecker;

    public UpdateCartItemUseCase(CartRepository cartRepository, VariantSaleChecker variantSaleChecker) {
        this.cartRepository = cartRepository;
        this.variantSaleChecker = variantSaleChecker;
    }

    /**
     * Sets the quantity of the variant in the user's cart, adding it (and the cart itself) if it is not there yet.
     *
     * @throws InvalidOperationException if the variant does not exist or its product is not published
     * @throws InvalidArgumentException  if the quantity is not between 1 and {@link CartItem#MAX_QUANTITY}
     */
    public void execute(UpdateCartItemCommand command) {
        VariantId variantId = VariantId.of(command.variantId());
        if (!variantSaleChecker.checkNotForSale(List.of(variantId)).isEmpty()) {
            throw new InvalidOperationException("Variant " + variantId + " is not for sale");
        }

        UserId userId = UserId.of(command.userId());
        cartRepository.lockForUser(userId);
        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> Cart.create(userId));

        cart.updateQuantity(variantId, command.quantity());
        cartRepository.save(cart);
    }
}
