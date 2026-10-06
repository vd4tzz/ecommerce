package io.d4tzz.ecommerce.inventory.domain;

import java.util.List;

public interface StockReservationRepository {
    boolean exists(Long orderId, WarehouseId warehouseId);

    /** Loads and pessimistically locks all reservations of the order, ordered by warehouse id. */
    List<StockReservation> findAllForUpdate(Long orderId);

    StockReservation save(StockReservation reservation);
}
