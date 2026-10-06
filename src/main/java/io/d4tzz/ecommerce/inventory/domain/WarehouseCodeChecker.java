package io.d4tzz.ecommerce.inventory.domain;

@FunctionalInterface
public interface WarehouseCodeChecker {
    boolean exists(WarehouseCode code);
}
