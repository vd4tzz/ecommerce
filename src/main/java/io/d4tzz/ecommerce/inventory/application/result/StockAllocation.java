package io.d4tzz.ecommerce.inventory.application.result;

import java.util.List;

/** The part of an order one warehouse sends. */
public record StockAllocation(Long warehouseId, List<Line> lines) {
    public record Line(Long variantId, int quantity) {}
}
