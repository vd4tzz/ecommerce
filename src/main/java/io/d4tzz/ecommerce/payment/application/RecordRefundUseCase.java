package io.d4tzz.ecommerce.payment.application;

import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.common.exception.ConflictException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.payment.application.command.RecordRefundCommand;
import io.d4tzz.ecommerce.payment.domain.Payment;
import io.d4tzz.ecommerce.payment.domain.PaymentId;
import io.d4tzz.ecommerce.payment.domain.PaymentRepository;
import io.d4tzz.ecommerce.payment.domain.PaymentType;
import io.d4tzz.ecommerce.payment.domain.event.RefundRecordedEvent;
import java.time.Instant;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records money sent back to the customer for one payment. Returns the refund id.
 *
 * <p>The refunded payment is locked so concurrent refunds cannot exceed the amount received. Idempotent per
 * transaction number, like {@link RecordPaymentUseCase}.
 */
@Service
@Transactional
public class RecordRefundUseCase {

    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public RecordRefundUseCase(PaymentRepository paymentRepository, ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
    }

    public Long execute(RecordRefundCommand command) {
        // Get the payment need to refund.
        Payment payment = paymentRepository
            .findForUpdate(PaymentId.of(command.paymentId()))
            .orElseThrow(() -> new NotFoundException("Payment not found with id: " + command.paymentId()));
        Money amount = Money.of(command.amount());

        // Idempotency: We may get the same refund twice, for example when the provider sends it again.
        // Each transaction number is one transfer, so check if we already saved this number.
        // If there is no transaction number, we cannot check, so we save it as a new refund.
        Optional<Payment> existing =
            command.transactionNo() == null || command.transactionNo().isBlank()
                ? Optional.empty()
                : paymentRepository.findByTransaction(command.method(), command.shopAccount(), command.transactionNo());
        if (existing.isPresent()) {
            Payment refund = existing.get();
            if (
                !refund.isSameAs(payment.getOrderReference(), PaymentType.REFUND, amount) ||
                !payment.getId().equals(refund.getRefundOf())
            ) {
                throw new ConflictException(
                    "Transaction " +
                        command.transactionNo() +
                        " is already recorded as " +
                        refund.describe() +
                        ", but this request is a REFUND of " +
                        amount +
                        " for order " +
                        payment.getOrderReference() +
                        ", refunding payment #" +
                        payment.getId()
                );
            }
            return refund.getId().value();
        }

        // This payment may already have been partly refunded, so get the total of the earlier refunds
        Money alreadyRefunded = paymentRepository.refundedAmount(payment.getId());
        Payment refund = paymentRepository.add(
            payment.refund(
                command.method(),
                command.shopAccount(),
                amount,
                alreadyRefunded,
                command.transactionNo(),
                command.note(),
                Instant.now()
            )
        );

        boolean fullyRefunded = alreadyRefunded.plus(amount).equals(payment.getAmount());

        // Order site consumes this event and mark refunded to the order
        eventPublisher.publishEvent(
            new RefundRecordedEvent(
                refund.getId(),
                payment.getId(),
                refund.getOrderReference(),
                refund.getMethod(),
                refund.getShopAccount(),
                refund.getAmount(),
                fullyRefunded,
                refund.getCreatedAt()
            )
        );
        return refund.getId().value();
    }
}
