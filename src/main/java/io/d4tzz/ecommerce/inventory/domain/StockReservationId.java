package io.d4tzz.ecommerce.inventory.domain;

import io.d4tzz.ecommerce.common.Identifier;

public class StockReservationId extends Identifier<Long> {

    public StockReservationId(Long id) {
        super(id);
    }

    public static StockReservationId of(Long id) {
        return new StockReservationId(id);
    }
}
