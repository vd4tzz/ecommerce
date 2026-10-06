package io.d4tzz.ecommerce.inventory.domain;

import io.d4tzz.ecommerce.common.Identifier;

public class InventoryItemId extends Identifier<Long> {

    public InventoryItemId(Long id) {
        super(id);
    }

    public static InventoryItemId of(Long id) {
        return new InventoryItemId(id);
    }
}
