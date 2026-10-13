package io.d4tzz.ecommerce.inventory.application.command;

import jakarta.validation.constraints.NotBlank;

/** The code is trimmed and upper-cased. */
public record CreateWarehouseCommand(
    @NotBlank String code,
    @NotBlank String name,
    @NotBlank String province,
    @NotBlank String district,
    @NotBlank String ward,
    @NotBlank String detailAddress
) {}
