package io.d4tzz.ecommerce.order.application.port;

import io.d4tzz.ecommerce.common.Money;
import java.time.Instant;
import java.util.Map;

/** Lets order ask the payment side to collect the money for an order through the gateway the customer picked. */
public interface PaymentGateway {
    /** The client sends {@code params}, unchanged, to {@code url} with {@code httpMethod}. */
    record PaymentRedirect(String httpMethod, String url, Map<String, String> params) {}

    /**
     * @param expiresAt when the order stops accepting payment; null when there is no deadline
     * @param clientIp  the customer's IP address, which some gateways require; null when unknown
     * @param gateway   e.g. "SEPAY"; fails when no such gateway is available
     */
    record PaymentRequest(String orderCode, Money amount, Instant expiresAt, String clientIp, String gateway) {}

    PaymentRedirect request(PaymentRequest request);
}
