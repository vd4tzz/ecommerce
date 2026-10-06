package io.d4tzz.ecommerce.payment.domain.event;

import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.payment.domain.PaymentId;
import io.d4tzz.ecommerce.payment.domain.PaymentMethod;
import java.time.Instant;

/** {@code fullyRefunded} is true when the refunded payment has now been returned in full. */
public record RefundRecordedEvent(
    PaymentId refundId,
    PaymentId paymentId,
    String orderReference,
    PaymentMethod method,
    String shopAccount,
    Money amount,
    boolean fullyRefunded,
    Instant occurredAt
) {}
