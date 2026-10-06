package io.d4tzz.ecommerce.inventory.query;

import java.util.List;
import java.util.Optional;

public interface WarehouseQueryService {
    List<WarehouseView> getWarehouses();

    Optional<WarehouseView> getWarehouse(Long id);
}
