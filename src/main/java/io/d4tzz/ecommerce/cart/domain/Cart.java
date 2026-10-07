package io.d4tzz.ecommerce.cart.domain;

import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import io.d4tzz.ecommerce.user.domain.UserId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Cart {

    private CartId id;
    private final UserId userId;
    private final List<CartItem> items;
    private Map<VariantId, CartItem> itemIndex;

    public Cart(CartId id, UserId userId, List<CartItem> items) {
        this.id = id;
        this.userId = userId;
        this.items = items;
    }

    public static Cart create(UserId userId) {
        return new Cart(null, userId, new ArrayList<>());
    }

    public void addItem(VariantId variantId, int quantity) {
        Optional<CartItem> existingItem = findItem(variantId);
        if (existingItem.isPresent()) {
            existingItem.get().increaseQuantity(quantity);
        } else {
            _add(variantId, quantity);
        }
    }

    /** Sets the quantity of the variant, adding it when it is not in the cart yet. Use {@link #removeItem} to remove. */
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

    public int getTotalQuantity() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

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
