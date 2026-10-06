package io.d4tzz.ecommerce.payment.application;

import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.payment.application.command.AssignPaymentToOrderCommand;
import io.d4tzz.ecommerce.payment.domain.Payment;
import io.d4tzz.ecommerce.payment.domain.PaymentId;
import io.d4tzz.ecommerce.payment.domain.PaymentRepository;
import io.d4tzz.ecommerce.payment.domain.event.PaymentReceivedEvent;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A person found which order an unmatched payment is for (e.g. the customer mistyped the code in the transfer)
 * and assigns it. The order then gets the payment as if it had just arrived: it is accepted, or refunded when the
 * order cannot take it.
 *
 * <p>A payment is assigned once, so check the code first. The order is not checked here, since payment does not
 * read orders: if no order has that code, the order side only logs it and the payment keeps that code.
 */
@Service
@Transactional
public class AssignPaymentToOrderUseCase {

    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AssignPaymentToOrderUseCase(PaymentRepository paymentRepository, ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
    }

    public void execute(AssignPaymentToOrderCommand command) {
        Payment payment = paymentRepository
            .findForUpdate(PaymentId.of(command.paymentId()))
            .orElseThrow(() -> new NotFoundException("Payment not found with id: " + command.paymentId()));

        payment.assignToOrder(command.orderCode());
        paymentRepository.saveOrderReference(payment);

        // Same event as a payment that arrives with its order code, so the order side handles both alike
        eventPublisher.publishEvent(
            new PaymentReceivedEvent(
                payment.getId(),
                payment.getOrderReference(),
                payment.getMethod(),
                payment.getShopAccount(),
                payment.getAmount(),
                payment.getTransactionNo(),
                Instant.now()
            )
        );
    }
}
