package io.d4tzz.ecommerce.inventory.domain;

import java.util.List;

/** Reservations of orders. */
public interface StockReservationRepository {
    /** Whether the order already holds stock in this warehouse. */
    boolean exists(Long orderId, WarehouseId warehouseId);

    /** Loads and pessimistically locks all reservations of the order, ordered by warehouse id. */
    List<StockReservation> findAllForUpdate(Long orderId);

    /** Inserts a new reservation, or updates the status of an existing one. Lines never change. */
    StockReservation save(StockReservation reservation);
}
