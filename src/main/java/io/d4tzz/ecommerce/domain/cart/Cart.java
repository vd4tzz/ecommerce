package io.d4tzz.ecommerce.domain.cart;

import io.d4tzz.ecommerce.domain.catalog.product.VariantId;
import io.d4tzz.ecommerce.domain.user.UserId;
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
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
    }

    public static Cart create(UserId userId) {
        return new Cart(null, userId, new ArrayList<>());
    }

    public static Cart restore(CartId id, UserId userId, List<CartItem> items) {
        return new Cart(id, userId, items);
    }

    public void addItem(VariantId variantId, int quantity) {
        Optional<CartItem> existingItem = findItem(variantId);
        if (existingItem.isPresent()) {
            existingItem.get().increaseQuantity(quantity);
        } else {
            _add(variantId, quantity);
        }
    }

    public void updateQuantity(VariantId variantId, int newQuantity) {
        if (newQuantity <= 0) {
            removeItem(variantId);
            return;
        }

        Optional<CartItem> existingItem = findItem(variantId);
        if (existingItem.isPresent()) {
            existingItem.get().changeQuantity(newQuantity);
        } else {
            _add(variantId, newQuantity);
        }
    }

    public void removeItem(VariantId variantId) {
        removeExistingItem(variantId);
    }

    public void clear() {
        items.clear();
        itemIndex = null;
    }

    public void mergeItem(VariantId variantId, int quantity) {
        if (quantity > 0 && findItem(variantId).isEmpty()) {
            _add(variantId, quantity);
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
