package io.d4tzz.ecommerce.payment.infrastructure.persistence.repository;

import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.payment.domain.PaymentId;
import io.d4tzz.ecommerce.payment.domain.RefundRequest;
import io.d4tzz.ecommerce.payment.domain.RefundRequestId;
import io.d4tzz.ecommerce.payment.domain.RefundRequestRepository;
import io.d4tzz.ecommerce.payment.domain.RefundRequestStatus;
import io.d4tzz.ecommerce.payment.infrastructure.persistence.entity.JpaRefundRequest;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class RefundRequestRepositoryImpl implements RefundRequestRepository {

    private final SpringRefundRequestRepository springRepo;

    public RefundRequestRepositoryImpl(SpringRefundRequestRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public Optional<RefundRequest> findForUpdate(RefundRequestId id) {
        return springRepo.findForUpdate(id.value()).map(this::toDomain);
    }

    @Override
    public boolean existsPending(PaymentId paymentId) {
        return springRepo.existsByPaymentIdAndStatus(paymentId.value(), RefundRequestStatus.PENDING);
    }

    @Override
    public RefundRequest save(RefundRequest request) {
        JpaRefundRequest jpa;
        if (request.getId() == null) {
            jpa = new JpaRefundRequest();
            jpa.setPaymentId(request.getPaymentId().value());
            jpa.setAmount(request.getAmount().amount());
            jpa.setReason(request.getReason());
            jpa.setCreatedAt(request.getCreatedAt());
        } else {
            jpa = springRepo.findById(request.getId().value()).orElseThrow();
        }
        jpa.setStatus(request.getStatus());
        jpa.setRefundId(request.getRefundId() != null ? request.getRefundId().value() : null);
        jpa.setCompletedAt(request.getCompletedAt());
        return toDomain(springRepo.save(jpa));
    }

    private RefundRequest toDomain(JpaRefundRequest jpa) {
        return RefundRequest.restore(
            RefundRequestId.of(jpa.getId()),
            PaymentId.of(jpa.getPaymentId()),
            Money.of(jpa.getAmount()),
            jpa.getReason(),
            jpa.getStatus(),
            jpa.getRefundId() != null ? PaymentId.of(jpa.getRefundId()) : null,
            jpa.getCreatedAt(),
            jpa.getCompletedAt()
        );
    }
}
