package io.d4tzz.ecommerce.shipping.application.port;

import io.d4tzz.ecommerce.common.Address;
import java.util.Collection;
import java.util.Map;

/** Where shipping looks up warehouse information. */
public interface WarehouseLookup {
    /** The address of each warehouse, by id. Warehouses that do not exist are left out. */
    Map<Long, Address> addressesOf(Collection<Long> warehouseIds);
}
