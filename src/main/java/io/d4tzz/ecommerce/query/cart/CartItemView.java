package io.d4tzz.ecommerce.query.cart;

import java.math.BigDecimal;

public record CartItemView(
    Long id,
    Long variantId,
    String sku,
    String productName,
    String productSlug,
    String mediaUrl,
    BigDecimal unitPrice,
    int quantity,
    BigDecimal totalPrice
) {}
