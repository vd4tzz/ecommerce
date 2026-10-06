package io.d4tzz.ecommerce.inventory.application.command;

import jakarta.validation.constraints.NotNull;

public record ReleaseStockCommand(@NotNull Long orderId) {}
