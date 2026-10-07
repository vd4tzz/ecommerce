package io.d4tzz.ecommerce.order.inbound.listener;

import io.d4tzz.ecommerce.order.application.MarkOrderPaidUseCase;
import io.d4tzz.ecommerce.order.application.command.MarkOrderPaidCommand;
import io.d4tzz.ecommerce.payment.domain.event.PaymentReceivedEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class MarkOrderPaidOnPaymentReceivedListener {

    private final MarkOrderPaidUseCase markOrderPaidUseCase;

    public MarkOrderPaidOnPaymentReceivedListener(MarkOrderPaidUseCase markOrderPaidUseCase) {
        this.markOrderPaidUseCase = markOrderPaidUseCase;
    }

    @ApplicationModuleListener
    public void onPaymentReceived(PaymentReceivedEvent event) {
        if (event.orderReference() == null) {
            // Unmatched money: nothing for the order side until a person assigns it to an order
            return;
        }
        markOrderPaidUseCase.execute(
            new MarkOrderPaidCommand(event.orderReference(), event.paymentId().value(), event.amount().amount())
        );
    }
}
