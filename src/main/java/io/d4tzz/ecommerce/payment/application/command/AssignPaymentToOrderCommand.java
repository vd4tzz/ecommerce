package io.d4tzz.ecommerce.payment.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AssignPaymentToOrderCommand(@NotNull Long paymentId, @NotBlank @Size(max = 50) String orderCode) {}
