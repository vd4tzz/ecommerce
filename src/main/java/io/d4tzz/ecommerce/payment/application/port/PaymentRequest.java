package io.d4tzz.ecommerce.payment.application.port;

import io.d4tzz.ecommerce.common.Money;
import java.time.Instant;
import java.util.Objects;

/**
 * What a gateway may need to collect the money for an order. Each gateway uses the parts it needs and ignores
 * the rest, so callers never have to know which gateway wants what.
 *
 * @param orderCode the order the money is for; the gateway passes it back when it reports the payment
 * @param amount    how much to collect
 * @param expiresAt when the order stops accepting payment, so the gateway can close the payment then; null when
 *                  there is no deadline
 * @param clientIp  the customer's IP address (some gateways require it); null when unknown
 */
public record PaymentRequest(String orderCode, Money amount, Instant expiresAt, String clientIp) {
    public PaymentRequest {
        Objects.requireNonNull(orderCode, "orderCode must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
    }
}
