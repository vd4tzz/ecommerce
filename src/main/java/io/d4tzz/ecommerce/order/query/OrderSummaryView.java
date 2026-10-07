package io.d4tzz.ecommerce.order.query;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * {@code paymentExpired} is computed from the deadline at read time, so it is already true before the expiry
 * job cancels the order.
 */
public record OrderSummaryView(
    Long id,
    String orderCode,
    Long userId,
    String recipientName,
    String recipientPhone,
    int totalQuantity,
    BigDecimal totalAmount,
    String status,
    String paymentStatus,
    Instant paymentExpiresAt,
    boolean paymentExpired,
    Instant createdAt
) {}
