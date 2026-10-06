package io.d4tzz.ecommerce.payment.query;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A refund request with what the person sending the money needs: the order, and how the customer paid
 * ({@code paymentMethod}, {@code paymentShopAccount}, {@code paymentTransactionNo}), which tells where to look up
 * the customer's account. {@code refundId} and {@code completedAt} are null while PENDING.
 */
public record RefundRequestView(
    Long id,
    Long paymentId,
    String orderReference,
    String paymentMethod,
    String paymentShopAccount,
    String paymentTransactionNo,
    BigDecimal amount,
    String reason,
    String status,
    Long refundId,
    Instant createdAt,
    Instant completedAt
) {}
