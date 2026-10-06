package io.d4tzz.ecommerce.payment.application;

import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.payment.application.command.RequestPaymentCommand;
import io.d4tzz.ecommerce.payment.application.port.PaymentGateways;
import io.d4tzz.ecommerce.payment.application.port.PaymentRedirect;
import io.d4tzz.ecommerce.payment.application.port.PaymentRequest;
import org.springframework.stereotype.Service;

/**
 * Asks the gateway the customer picked to collect the money for an order, and returns where to send the
 * customer. Nothing is saved: the money is recorded only when the gateway reports it (see
 * {@link RecordPaymentUseCase}), so this can be called again as often as needed, with the same gateway or
 * another. Whether the order can still be paid is the order's decision, checked before calling this.
 */
@Service
public class RequestPaymentUseCase {

    private final PaymentGateways paymentGateways;

    public RequestPaymentUseCase(PaymentGateways paymentGateways) {
        this.paymentGateways = paymentGateways;
    }

    public PaymentRedirect execute(RequestPaymentCommand command) {
        PaymentRequest request = new PaymentRequest(
            command.orderCode(),
            Money.of(command.amount()),
            command.expiresAt(),
            command.clientIp()
        );
        return paymentGateways.get(command.gateway()).requestPayment(request);
    }
}
