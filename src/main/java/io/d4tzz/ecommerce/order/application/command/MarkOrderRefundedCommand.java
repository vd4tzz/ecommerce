package io.d4tzz.ecommerce.order.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** {@code paymentId} is the payment that was refunded in full, as recorded by the payment module. */
public record MarkOrderRefundedCommand(@NotBlank String orderCode, @NotNull Long paymentId) {}
