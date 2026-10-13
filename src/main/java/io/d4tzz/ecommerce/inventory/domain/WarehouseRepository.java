package io.d4tzz.ecommerce.inventory.domain;

import java.util.Optional;

/** Warehouses. */
public interface WarehouseRepository {
    Optional<Warehouse> find(WarehouseId id);

    boolean existsByCode(WarehouseCode code);

    Warehouse save(Warehouse warehouse);
}
