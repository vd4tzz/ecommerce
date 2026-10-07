package io.d4tzz.ecommerce.order.infrastructure.payment;

import io.d4tzz.ecommerce.order.application.port.PaymentRefunds;
import io.d4tzz.ecommerce.payment.application.RequestRefundUseCase;
import io.d4tzz.ecommerce.payment.application.command.RequestRefundCommand;
import org.springframework.stereotype.Component;

/** Calls the payment module directly; runs in the caller's transaction. */
@Component
public class PaymentModuleRefunds implements PaymentRefunds {

    private final RequestRefundUseCase requestRefundUseCase;

    public PaymentModuleRefunds(RequestRefundUseCase requestRefundUseCase) {
        this.requestRefundUseCase = requestRefundUseCase;
    }

    @Override
    public void requestRefund(Long paymentId, String reason) {
        requestRefundUseCase.execute(new RequestRefundCommand(paymentId, reason));
    }
}
