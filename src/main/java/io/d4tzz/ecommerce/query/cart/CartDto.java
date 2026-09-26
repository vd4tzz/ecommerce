package io.d4tzz.ecommerce.query.cart;

import java.math.BigDecimal;
import java.util.List;

public record CartDto(
    Long id,
    Long userId,
    List<CartItemDto> items,
    int totalQuantity,
    BigDecimal subtotal
) {
    public static CartDto empty(Long userId) {
        return new CartDto(null, userId, List.of(), 0, BigDecimal.ZERO);
    }
}
