package io.d4tzz.ecommerce.inventory.application.command;

import jakarta.validation.constraints.NotNull;

/** The order was cancelled or expired before shipping. */
public record ReleaseStockCommand(@NotNull Long orderId) {}
