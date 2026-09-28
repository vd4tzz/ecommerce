package io.d4tzz.ecommerce.query.cart;

import java.math.BigDecimal;
import java.util.List;

public record CartView(Long id, Long userId, List<CartItemView> items, int totalQuantity, BigDecimal subtotal) {}
