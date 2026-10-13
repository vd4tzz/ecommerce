package io.d4tzz.ecommerce.cart.application;

import io.d4tzz.ecommerce.cart.domain.CartItem;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.cart.application.command.AddToCartCommand;
import io.d4tzz.ecommerce.cart.domain.Cart;
import io.d4tzz.ecommerce.cart.domain.CartRepository;
import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import io.d4tzz.ecommerce.catalog.published.VariantSaleChecker;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.user.domain.UserId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Adds a variant to the user's cart, creating the cart if needed. Only variants for sale can be added. */
@Service
@Transactional
public class AddToCartUseCase {

    private final CartRepository cartRepository;
    private final VariantSaleChecker variantSaleChecker;

    public AddToCartUseCase(CartRepository cartRepository, VariantSaleChecker variantSaleChecker) {
        this.cartRepository = cartRepository;
        this.variantSaleChecker = variantSaleChecker;
    }

    /**
     * Adds {@code quantity} of the variant to the user's cart. If the cart already has the variant, the quantity is
     * added to the existing item.
     *
     * @throws InvalidOperationException if the variant does not exist or its product is not published
     * @throws InvalidArgumentException  if the item would hold more than {@link CartItem#MAX_QUANTITY}
     */
    public void execute(AddToCartCommand command) {
        VariantId variantId = VariantId.of(command.variantId());
        if (!variantSaleChecker.checkNotForSale(List.of(variantId)).isEmpty()) {
            throw new InvalidOperationException("Variant " + variantId + " is not for sale");
        }

        UserId userId = UserId.of(command.userId());
        cartRepository.lockForUser(userId);
        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> Cart.create(userId));

        cart.addItem(variantId, command.quantity());
        cartRepository.save(cart);
    }
}
