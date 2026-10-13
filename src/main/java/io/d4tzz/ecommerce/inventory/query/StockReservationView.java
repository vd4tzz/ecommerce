package io.d4tzz.ecommerce.inventory.query;

import java.time.Instant;
import java.util.List;

/** Stock held for an order in one warehouse. {@code status} is a ReservationStatus. */
public record StockReservationView(
    Long id,
    Long orderId,
    Long warehouseId,
    String warehouseCode,
    String status,
    List<Line> lines,
    Instant createdAt,
    Instant updatedAt
) {
    public record Line(Long variantId, String sku, int quantity) {}
}
