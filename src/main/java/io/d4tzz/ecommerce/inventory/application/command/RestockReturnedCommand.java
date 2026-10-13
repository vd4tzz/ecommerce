package io.d4tzz.ecommerce.inventory.application.command;

import jakarta.validation.constraints.NotNull;

/** The shipped goods of the order came back. */
public record RestockReturnedCommand(@NotNull Long orderId) {}
