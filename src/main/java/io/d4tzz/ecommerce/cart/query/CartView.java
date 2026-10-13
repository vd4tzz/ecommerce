package io.d4tzz.ecommerce.cart.query;

import java.math.BigDecimal;
import java.util.List;

/**
 * A cart as shown to the user.
 *
 * @param id            cart id, {@code null} when the cart has no items
 * @param userId        the owner, {@code null} for a visitor
 * @param items         the items, oldest first
 * @param totalQuantity the number of units in the cart: the sum of all item quantities
 * @param subtotal      the sum of every item's total price, before shipping
 */
public record CartView(Long id, Long userId, List<CartItemView> items, int totalQuantity, BigDecimal subtotal) {}
