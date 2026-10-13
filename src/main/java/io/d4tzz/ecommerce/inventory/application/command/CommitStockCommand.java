package io.d4tzz.ecommerce.inventory.application.command;

import jakarta.validation.constraints.NotNull;

/** The goods of the order left the warehouse. */
public record CommitStockCommand(@NotNull Long orderId) {}
