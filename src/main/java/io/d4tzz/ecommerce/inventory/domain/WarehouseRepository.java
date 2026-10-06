package io.d4tzz.ecommerce.inventory.domain;

import java.util.Optional;

public interface WarehouseRepository {
    Optional<Warehouse> find(WarehouseId id);

    boolean existsByCode(WarehouseCode code);

    Warehouse save(Warehouse warehouse);
}
