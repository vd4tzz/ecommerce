package io.d4tzz.ecommerce.inventory.domain;

/** Tells whether a warehouse code is already taken. */
@FunctionalInterface
public interface WarehouseCodeChecker {
    boolean exists(WarehouseCode code);
}
