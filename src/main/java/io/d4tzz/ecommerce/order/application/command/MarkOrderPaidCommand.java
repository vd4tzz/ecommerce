package io.d4tzz.ecommerce.order.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/** {@code paymentId} is the payment recorded by the payment module. */
public record MarkOrderPaidCommand(
    @NotBlank String orderCode,
    @NotNull Long paymentId,
    @NotNull @Positive BigDecimal amount
) {}
