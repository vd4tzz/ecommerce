package io.d4tzz.ecommerce.payment.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** {@code reason} is free text from the requester, e.g. why an order did not accept the payment. */
public record RequestRefundCommand(@NotNull Long paymentId, @NotBlank String reason) {}
