package io.d4tzz.ecommerce.cart.domain;

import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import io.d4tzz.ecommerce.user.domain.UserId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A user's shopping cart: the variants they intend to buy and how many of each. A cart holds each variant at most
 * once, and every item's quantity stays between 1 and {@link CartItem#MAX_QUANTITY}. Prices are not stored here;
 * they are read from the catalog when the cart is shown.
 */
public class Cart {

    private CartId id;
    private final UserId userId;
    private final List<CartItem> items;
    private Map<VariantId, CartItem> itemIndex;

    /**
     * Rebuilds a cart from persisted data without any validation.
     *
     * <p>To start a new cart use {@link #create}.
     *
     * @param id    cart id, {@code null} if not yet persisted
     * @param items the cart's items; the list must be mutable
     */
    public Cart(CartId id, UserId userId, List<CartItem> items) {
        this.id = id;
        this.userId = userId;
        this.items = items;
    }

    /** Creates a new, empty cart that is not yet persisted. */
    public static Cart create(UserId userId) {
        return new Cart(null, userId, new ArrayList<>());
    }

    /**
     * Adds {@code quantity} of a variant. If the variant is already in the cart, the quantity is added to the existing
     * item.
     *
     * @throws InvalidArgumentException if the quantity is out of range, or the item would exceed
     *                                  {@link CartItem#MAX_QUANTITY}
     */
    public void addItem(VariantId variantId, int quantity) {
        Optional<CartItem> existingItem = findItem(variantId);
        if (existingItem.isPresent()) {
            existingItem.get().increaseQuantity(quantity);
        } else {
            _add(variantId, quantity);
        }
    }

    /**
     * Sets the quantity of the variant, adding it when it is not in the cart yet. Use {@link #removeItem} to remove.
     *
     * @throws InvalidArgumentException if the quantity is not between 1 and {@link CartItem#MAX_QUANTITY}
     */
    public void updateQuantity(VariantId variantId, int newQuantity) {
        Optional<CartItem> existingItem = findItem(variantId);
        if (existingItem.isPresent()) {
            existingItem.get().changeQuantity(newQuantity);
        } else {
            _add(variantId, newQuantity);
        }
    }

    /** Does nothing when the variant is not in the cart. */
    public void removeItem(VariantId variantId) {
        removeExistingItem(variantId);
    }

    /** Removes every item. */
    public void clear() {
        items.clear();
        itemIndex = null;
    }

    /**
     * Adds an item from another cart (e.g. the guest cart after login). Items already in this cart keep their
     * quantity; a quantity above the limit is lowered to {@link CartItem#MAX_QUANTITY} instead of failing.
     */
    public void mergeItem(VariantId variantId, int quantity) {
        if (quantity > 0 && findItem(variantId).isEmpty()) {
            _add(variantId, Math.min(quantity, CartItem.MAX_QUANTITY));
        }
    }

    private void _add(VariantId variantId, int quantity) {
        CartItem newItem = CartItem.create(variantId, quantity);
        items.add(newItem);
        if (itemIndex != null) {
            itemIndex.put(variantId, newItem);
        }
    }

    private boolean removeExistingItem(VariantId variantId) {
        boolean removed = items.removeIf(item -> item.getVariantId().equals(variantId));
        if (removed && itemIndex != null) {
            itemIndex.remove(variantId);
        }
        return removed;
    }

    /** Finds the item for a variant, if the cart has it. */
    public Optional<CartItem> findItem(VariantId variantId) {
        return Optional.ofNullable(getItemIndex().get(variantId));
    }

    private Map<VariantId, CartItem> getItemIndex() {
        if (itemIndex == null) {
            itemIndex = new HashMap<>(items.size());
            for (CartItem item : items) {
                itemIndex.put(item.getVariantId(), item);
            }
        }
        return itemIndex;
    }

    /** Returns the total number of units in the cart: the sum of all item quantities. */
    public int getTotalQuantity() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    /** Tells whether the cart has no items. */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    public CartId getId() {
        return id;
    }

    public UserId getUserId() {
        return userId;
    }

    public List<CartItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
