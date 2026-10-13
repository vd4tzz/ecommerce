package io.d4tzz.ecommerce.cart.query;

/** Reads a cart in the shape the screens need, with product details and prices from the catalog. */
public interface CartQueryService {
    /**
     * Returns the user's cart with its items, oldest first.
     *
     * @param userId the user, or {@code null} for a visitor
     * @return the cart; a user without a cart, or a {@code null} user, gets an empty one without an id
     */
    CartView getCart(Long userId);
}
