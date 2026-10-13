package io.d4tzz.ecommerce.inventory.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** New name and address of a warehouse. */
public record UpdateWarehouseCommand(
    @NotNull Long warehouseId,
    @NotBlank String name,
    @NotBlank String province,
    @NotBlank String district,
    @NotBlank String ward,
    @NotBlank String detailAddress
) {}
