package io.d4tzz.ecommerce.order.application.command;

import jakarta.validation.constraints.NotNull;

public record ReturnOrderCommand(@NotNull Long orderId) {}
