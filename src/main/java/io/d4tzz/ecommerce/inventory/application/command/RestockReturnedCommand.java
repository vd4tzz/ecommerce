package io.d4tzz.ecommerce.inventory.application.command;

import jakarta.validation.constraints.NotNull;

public record RestockReturnedCommand(@NotNull Long orderId) {}
