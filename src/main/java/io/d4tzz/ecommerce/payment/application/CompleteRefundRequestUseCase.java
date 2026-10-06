package io.d4tzz.ecommerce.payment.application;

import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.payment.application.command.CompleteRefundRequestCommand;
import io.d4tzz.ecommerce.payment.application.command.RecordRefundCommand;
import io.d4tzz.ecommerce.payment.domain.PaymentId;
import io.d4tzz.ecommerce.payment.domain.RefundRequest;
import io.d4tzz.ecommerce.payment.domain.RefundRequestId;
import io.d4tzz.ecommerce.payment.domain.RefundRequestRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Someone sent the money of a refund request back to the customer: records the REFUND (which tells the order)
 * and marks the request done. Returns the refund id. Done twice, it records nothing new.
 */
@Service
@Transactional
public class CompleteRefundRequestUseCase {

    private final RefundRequestRepository refundRequestRepository;
    private final RecordRefundUseCase recordRefundUseCase;

    public CompleteRefundRequestUseCase(
        RefundRequestRepository refundRequestRepository,
        RecordRefundUseCase recordRefundUseCase
    ) {
        this.refundRequestRepository = refundRequestRepository;
        this.recordRefundUseCase = recordRefundUseCase;
    }

    public Long execute(CompleteRefundRequestCommand command) {
        // Locked so a double click cannot record the refund twice
        RefundRequest request = refundRequestRepository
            .findForUpdate(RefundRequestId.of(command.refundRequestId()))
            .orElseThrow(() -> new NotFoundException("Refund request not found with id: " + command.refundRequestId()));
        if (!request.isPending()) {
            return request.getRefundId().value();
        }

        Long refundId = recordRefundUseCase.execute(
            new RecordRefundCommand(
                request.getPaymentId().value(),
                command.method(),
                command.shopAccount(),
                request.getAmount().amount(),
                command.transactionNo(),
                command.note()
            )
        );

        request.complete(PaymentId.of(refundId), Instant.now());
        refundRequestRepository.save(request);
        return refundId;
    }
}
