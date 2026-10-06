package io.d4tzz.ecommerce.inventory.query;

import java.time.Instant;

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
