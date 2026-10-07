package io.d4tzz.ecommerce.order.application.port;

import io.d4tzz.ecommerce.order.domain.OrderId;

/**
 * The stock reserved for an order in inventory. Each call works on every warehouse of the order and skips the
 * warehouses already done, so calling it twice is safe.
 */
public interface StockReservations {
    /** The order was cancelled before shipping: give the held stock back. */
    void release(OrderId orderId);

    /** The goods left the warehouse: take them out of stock for good. */
    void commit(OrderId orderId);

    /** The shipped goods came back: put them back into stock. */
    void restock(OrderId orderId);
}
