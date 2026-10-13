package io.d4tzz.ecommerce.cart.domain;

import io.d4tzz.ecommerce.common.Repository;
import io.d4tzz.ecommerce.user.domain.UserId;
import java.util.Optional;

/**
 * Persistence port for {@link Cart}. A user has at most one cart.
 */
public interface CartRepository extends Repository<Cart, CartId> {

    /** Finds the user's cart, with its items. */
    Optional<Cart> findByUserId(UserId userId);

    /**
     * Makes other transactions that change this user's cart wait until the current transaction ends. Call it
     * before reading the cart you are about to change. It works even before the cart exists, so two requests
     * cannot both create it.
     */
    void lockForUser(UserId userId);
}
