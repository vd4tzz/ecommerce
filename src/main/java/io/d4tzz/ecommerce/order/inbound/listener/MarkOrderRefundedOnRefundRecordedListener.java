package io.d4tzz.ecommerce.order.inbound.listener;

import io.d4tzz.ecommerce.order.application.MarkOrderRefundedUseCase;
import io.d4tzz.ecommerce.order.application.command.MarkOrderRefundedCommand;
import io.d4tzz.ecommerce.payment.domain.event.RefundRecordedEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** The order only tracks full refunds: a partial refund leaves it PAID. */
@Component
public class MarkOrderRefundedOnRefundRecordedListener {

    private final MarkOrderRefundedUseCase markOrderRefundedUseCase;

    public MarkOrderRefundedOnRefundRecordedListener(MarkOrderRefundedUseCase markOrderRefundedUseCase) {
        this.markOrderRefundedUseCase = markOrderRefundedUseCase;
    }

    @ApplicationModuleListener
    public void onRefundRecorded(RefundRecordedEvent event) {
        // ensure that the total amount of the order must be fully refunded
        if (!event.fullyRefunded() || event.orderReference() == null) {
            return;
        }
        markOrderRefundedUseCase.execute(
            new MarkOrderRefundedCommand(event.orderReference(), event.paymentId().value())
        );
    }
}
