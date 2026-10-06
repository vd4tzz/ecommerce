package io.d4tzz.ecommerce.payment.application.command;

import io.d4tzz.ecommerce.payment.domain.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** {@code paymentId} is the PAYMENT being refunded. */
public record RecordRefundCommand(
    @NotNull Long paymentId,
    @NotNull PaymentMethod method,
    @Size(max = 50) String shopAccount,
    @NotNull @Positive BigDecimal amount,
    @Size(max = 100) String transactionNo,
    @Size(max = 500) String note
) {}
