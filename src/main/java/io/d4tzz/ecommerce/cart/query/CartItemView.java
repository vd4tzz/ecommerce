package io.d4tzz.ecommerce.cart.query;

import java.math.BigDecimal;

/**
 * One line of a cart as shown to the user.
 *
 * @param id          cart item id
 * @param variantId   the variant
 * @param sku         the variant's SKU
 * @param productName name of the variant's product
 * @param productSlug slug of the variant's product, for a link to it
 * @param mediaUrl    the variant's image, or the product's first image if the variant has none; {@code null} if neither
 * @param unitPrice   the variant's current price
 * @param quantity    the number of units
 * @param totalPrice  {@code unitPrice} times {@code quantity}
 */
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
