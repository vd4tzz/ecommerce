package io.d4tzz.ecommerce.inventory.application.command;

import jakarta.validation.constraints.NotBlank;

public record CreateWarehouseCommand(
    @NotBlank String code,
    @NotBlank String name,
    @NotBlank String province,
    @NotBlank String district,
    @NotBlank String ward,
    @NotBlank String detailAddress
) {}
