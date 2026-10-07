package io.d4tzz.ecommerce.order.infrastructure.payment;

import io.d4tzz.ecommerce.order.application.port.PaymentGateway;
import io.d4tzz.ecommerce.payment.application.RequestPaymentUseCase;
import io.d4tzz.ecommerce.payment.application.command.RequestPaymentCommand;
import org.springframework.stereotype.Component;

/** Calls the payment module directly. */
@Component
public class PaymentModuleGateway implements PaymentGateway {

    private final RequestPaymentUseCase requestPaymentUseCase;

    public PaymentModuleGateway(RequestPaymentUseCase requestPaymentUseCase) {
        this.requestPaymentUseCase = requestPaymentUseCase;
    }

    @Override
    public PaymentRedirect request(PaymentRequest request) {
        var redirect = requestPaymentUseCase.execute(
            new RequestPaymentCommand(
                request.orderCode(),
                request.amount().amount(),
                request.expiresAt(),
                request.clientIp(),
                request.gateway()
            )
        );
        return new PaymentRedirect(redirect.httpMethod(), redirect.url(), redirect.params());
    }
}
