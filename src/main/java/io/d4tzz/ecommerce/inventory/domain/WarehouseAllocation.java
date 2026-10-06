package io.d4tzz.ecommerce.inventory.domain;

import java.util.List;

/** The part of an order one warehouse sends. */
public record WarehouseAllocation(WarehouseId warehouseId, List<ReservationLine> lines) {
    public WarehouseAllocation {
        lines = List.copyOf(lines);
    }
}
