package io.d4tzz.ecommerce.order.application.command;

import jakarta.validation.constraints.NotNull;

public record DeliverOrderCommand(@NotNull Long orderId) {}
