package io.d4tzz.ecommerce.order.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CancelMyOrderCommand(
    @NotNull Long userId,
    @NotBlank String orderCode,
    @NotBlank @Size(max = 500) String reason
) {}
