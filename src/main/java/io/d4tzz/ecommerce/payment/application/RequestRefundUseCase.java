package io.d4tzz.ecommerce.payment.application;

import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.payment.application.command.RequestRefundCommand;
import io.d4tzz.ecommerce.payment.domain.Payment;
import io.d4tzz.ecommerce.payment.domain.PaymentId;
import io.d4tzz.ecommerce.payment.domain.PaymentRepository;
import io.d4tzz.ecommerce.payment.domain.PaymentType;
import io.d4tzz.ecommerce.payment.domain.RefundRequest;
import io.d4tzz.ecommerce.payment.domain.RefundRequestRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Asks for a payment to be sent back in full, e.g. because the order did not accept it. Saves a PENDING
 * {@link RefundRequest} for a person to handle: they send the money from the shop's account, then mark the
 * request done with {@link CompleteRefundRequestUseCase}, which records the REFUND.
 *
 * <p>Does nothing when the payment already has a pending request or was already given back in full, so the same
 * request may arrive more than once.
 */
@Service
@Transactional
public class RequestRefundUseCase {

    private static final Logger log = LoggerFactory.getLogger(RequestRefundUseCase.class);

    private final PaymentRepository paymentRepository;
    private final RefundRequestRepository refundRequestRepository;

    public RequestRefundUseCase(PaymentRepository paymentRepository, RefundRequestRepository refundRequestRepository) {
        this.paymentRepository = paymentRepository;
        this.refundRequestRepository = refundRequestRepository;
    }

    public void execute(RequestRefundCommand command) {
        PaymentId paymentId = PaymentId.of(command.paymentId());

        // Locked so two requests for the same payment cannot both create a pending request
        Payment payment = paymentRepository
            .findForUpdate(paymentId)
            .orElseThrow(() -> new NotFoundException("Payment not found with id: " + command.paymentId()));
        if (payment.getType() != PaymentType.PAYMENT) {
            throw new InvalidOperationException("Only a payment can be refunded, not " + payment.describe());
        }

        if (refundRequestRepository.existsPending(paymentId)) {
            return;
        }
        Money toGiveBack = payment.getAmount().minus(paymentRepository.refundedAmount(paymentId));
        if (toGiveBack.isZero()) {
            return;
        }

        refundRequestRepository.save(RefundRequest.create(paymentId, toGiveBack, command.reason(), Instant.now()));
        log.info("Refund of {} requested for {}: {}", toGiveBack, payment.describe(), command.reason());
    }
}
