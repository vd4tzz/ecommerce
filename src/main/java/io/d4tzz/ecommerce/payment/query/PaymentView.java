package io.d4tzz.ecommerce.payment.query;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * {@code orderReference} is the order code stored on the payment (null when unmatched); {@code orderId} is the
 * matching order, null when no order has that code. {@code refundOf} is set on a REFUND. {@code refundedAmount} is the sum of the refunds of a PAYMENT and null on
 * a REFUND.
 */
public record PaymentView(
    Long id,
    String orderReference,
    Long orderId,
    String type,
    String method,
    String shopAccount,
    BigDecimal amount,
    BigDecimal refundedAmount,
    String transactionNo,
    Long refundOf,
    String note,
    Instant createdAt
) {}
