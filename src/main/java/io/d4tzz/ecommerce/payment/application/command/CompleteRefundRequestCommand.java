package io.d4tzz.ecommerce.payment.application.command;

import io.d4tzz.ecommerce.payment.domain.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * How the money was sent back. For a bank transfer: {@code method} BANK_TRANSFER, {@code shopAccount} the account
 * it was sent from, {@code transactionNo} the number the bank gave the transfer. For cash: {@code method} COD and
 * neither of the two.
 */
public record CompleteRefundRequestCommand(
    @NotNull Long refundRequestId,
    @NotNull PaymentMethod method,
    @Size(max = 50) String shopAccount,
    @Size(max = 100) String transactionNo,
    @Size(max = 500) String note
) {}
