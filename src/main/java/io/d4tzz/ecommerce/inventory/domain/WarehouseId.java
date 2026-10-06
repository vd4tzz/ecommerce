package io.d4tzz.ecommerce.inventory.domain;

import io.d4tzz.ecommerce.common.Identifier;

public class WarehouseId extends Identifier<Long> {

    public WarehouseId(Long id) {
        super(id);
    }

    public static WarehouseId of(Long id) {
        return new WarehouseId(id);
    }
}
