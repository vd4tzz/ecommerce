package io.d4tzz.ecommerce.payment.application.port;

import io.d4tzz.ecommerce.payment.domain.PaymentMethod;

/**
 * One gateway customers can pay through. Each gateway is its own adapter; {@link #method()} is how a request
 * picks it. How a gateway reports the result back (its webhook) is handled by that gateway's own controller.
 */
public interface PaymentGateway {
    PaymentMethod method();

    /**
     * Asks the gateway to collect the money for the order, and returns where to send the customer to pay.
     * Changes nothing on our side, so it can be called again for the same order. The gateway must pass the
     * order code back when it reports the payment, so the money can be matched to the order.
     */
    PaymentRedirect requestPayment(PaymentRequest request);
}
