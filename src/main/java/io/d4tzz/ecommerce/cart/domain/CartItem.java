package io.d4tzz.ecommerce.cart.domain;

import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.domain.catalog.product.VariantId;

/** One variant in a cart. Its quantity is always between 1 and {@link #MAX_QUANTITY}. */
public class CartItem {

    public static final int MAX_QUANTITY = 99;

    private CartItemId id;
    private final VariantId variantId;
    private int quantity;

    public CartItem(CartItemId id, VariantId variantId, int quantity) {
        requireValidQuantity(quantity);
        this.id = id;
        this.variantId = variantId;
        this.quantity = quantity;
    }

    public static CartItem create(VariantId variantId, int quantity) {
        return new CartItem(null, variantId, quantity);
    }

    public static CartItem restore(CartItemId id, VariantId variantId, int quantity) {
        return new CartItem(id, variantId, quantity);
    }

    public void increaseQuantity(int amount) {
        if (amount <= 0) {
            throw new InvalidArgumentException("Quantity increment must be greater than zero");
        }
        if (amount > MAX_QUANTITY - quantity) {
            throw new InvalidArgumentException(
                "A cart item can hold at most " + MAX_QUANTITY + ", it already has " + quantity
            );
        }
        this.quantity += amount;
    }

    public void changeQuantity(int newQuantity) {
        requireValidQuantity(newQuantity);
        this.quantity = newQuantity;
    }

    private static void requireValidQuantity(int quantity) {
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw new InvalidArgumentException("Item quantity must be between 1 and " + MAX_QUANTITY);
        }
    }

    public CartItemId getId() {
        return id;
    }

    public VariantId getVariantId() {
        return variantId;
    }

    public int getQuantity() {
        return quantity;
    }
}
