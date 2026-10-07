package io.d4tzz.ecommerce.order.infrastructure.payment;

import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.order.application.port.CashOnDeliveryPayments;
import io.d4tzz.ecommerce.payment.application.RecordPaymentUseCase;
import io.d4tzz.ecommerce.payment.application.command.RecordPaymentCommand;
import io.d4tzz.ecommerce.payment.domain.PaymentMethod;
import org.springframework.stereotype.Component;

/** Calls the payment module directly; runs in the caller's transaction. */
@Component
public class PaymentModuleCashOnDelivery implements CashOnDeliveryPayments {

    private final RecordPaymentUseCase recordPaymentUseCase;

    public PaymentModuleCashOnDelivery(RecordPaymentUseCase recordPaymentUseCase) {
        this.recordPaymentUseCase = recordPaymentUseCase;
    }

    @Override
    public void recordCollected(String orderCode, Money amount) {
        // Cash has no shop account and no transaction number
        recordPaymentUseCase.execute(
            new RecordPaymentCommand(orderCode, PaymentMethod.COD, null, amount.amount(), null, "Collected on delivery")
        );
    }
}
