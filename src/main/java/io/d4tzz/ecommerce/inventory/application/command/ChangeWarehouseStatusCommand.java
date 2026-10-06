package io.d4tzz.ecommerce.inventory.application.command;

import io.d4tzz.ecommerce.inventory.domain.WarehouseStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeWarehouseStatusCommand(@NotNull Long warehouseId, @NotNull WarehouseStatus status) {}
