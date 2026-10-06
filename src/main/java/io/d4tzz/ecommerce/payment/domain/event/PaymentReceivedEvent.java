package io.d4tzz.ecommerce.payment.domain.event;

import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.payment.domain.PaymentId;
import io.d4tzz.ecommerce.payment.domain.PaymentMethod;
import java.time.Instant;

/** {@code orderReference} is the order code, or null when the money could not be matched to an order. */
public record PaymentReceivedEvent(
    PaymentId paymentId,
    String orderReference,
    PaymentMethod method,
    String shopAccount,
    Money amount,
    String transactionNo,
    Instant occurredAt
) {}
