package io.d4tzz.ecommerce.inventory.query;

import java.time.Instant;

/** Stock of one variant in one warehouse; {@code available} is {@code onHand - reserved}. */
public record StockItemView(
    Long warehouseId,
    String warehouseCode,
    Long variantId,
    String sku,
    Long productId,
    String productName,
    int onHand,
    int reserved,
    int available,
    Instant updatedAt
) {}
