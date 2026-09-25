package io.d4tzz.ecommerce.domain.cart;

import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.domain.catalog.product.VariantId;
public class CartItem {

    private CartItemId id;
    private final VariantId variantId;
    private int quantity;

    public CartItem(CartItemId id, VariantId variantId, int quantity) {
        if (quantity <= 0) {
            throw new InvalidArgumentException("Item quantity must be greater than zero");
        }
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
        this.quantity += amount;
    }

    public void changeQuantity(int newQuantity) {
        if (newQuantity <= 0) {
            throw new InvalidArgumentException("Item quantity must be greater than zero");
        }
        this.quantity = newQuantity;
    }

    public CartItemId getId() {
        return id;
    }

    public void setId(CartItemId id) {
        this.id = id;
    }

    public VariantId getVariantId() {
        return variantId;
    }

    public int getQuantity() {
        return quantity;
    }
}
