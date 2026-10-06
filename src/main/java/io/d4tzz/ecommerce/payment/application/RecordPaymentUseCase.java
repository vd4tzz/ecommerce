package io.d4tzz.ecommerce.payment.application;

import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.common.exception.ConflictException;
import io.d4tzz.ecommerce.payment.application.command.RecordPaymentCommand;
import io.d4tzz.ecommerce.payment.domain.Payment;
import io.d4tzz.ecommerce.payment.domain.PaymentRepository;
import io.d4tzz.ecommerce.payment.domain.PaymentType;
import io.d4tzz.ecommerce.payment.domain.event.PaymentReceivedEvent;
import java.time.Instant;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records money received for an order. Called by the provider adapters (gateway callback, bank statement
 * webhook, COD settlement). Returns the payment id.
 *
 * <p>The order is not checked here: the money has moved whatever the order id says, so it is always recorded and
 * the order side decides whether to accept it.
 *
 * <p>Idempotent per transaction number: the same transaction recorded again returns the existing payment and
 * publishes nothing.
 */
@Service
@Transactional
public class RecordPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public RecordPaymentUseCase(PaymentRepository paymentRepository, ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
    }

    public Long execute(RecordPaymentCommand command) {
        String orderReference = command.orderReference();
        Money amount = Money.of(command.amount());

        Optional<Payment> existing = findExisting(command);
        if (existing.isPresent()) {
            if (!existing.get().isSameAs(orderReference, PaymentType.PAYMENT, amount)) {
                throw new ConflictException(
                    "Transaction " + command.transactionNo() + " is already recorded as " +
                        existing.get().describe() + ", but this request is a PAYMENT of " + amount +
                        " for order " + orderReference
                );
            }
            return existing.get().getId().value();
        }

        // A concurrent duplicate that slips past the check fails on the unique transaction constraint
        Payment payment = paymentRepository.add(
            Payment.receive(
                orderReference,
                command.method(),
                command.shopAccount(),
                amount,
                command.transactionNo(),
                command.note(),
                Instant.now()
            )
        );

        // Order site consumes this event to update order status
        eventPublisher.publishEvent(
            new PaymentReceivedEvent(
                payment.getId(),
                payment.getOrderReference(),
                payment.getMethod(),
                payment.getShopAccount(),
                payment.getAmount(),
                payment.getTransactionNo(),
                payment.getCreatedAt()
            )
        );
        return payment.getId().value();
    }

    private Optional<Payment> findExisting(RecordPaymentCommand command) {
        if (command.transactionNo() == null || command.transactionNo().isBlank()) {
            return Optional.empty();
        }
        return paymentRepository.findByTransaction(command.method(), command.shopAccount(), command.transactionNo());
    }
}
