package io.d4tzz.ecommerce.payment.infrastructure.persistence.repository;

import io.d4tzz.ecommerce.payment.domain.RefundRequestStatus;
import io.d4tzz.ecommerce.payment.infrastructure.persistence.entity.JpaRefundRequest;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringRefundRequestRepository extends JpaRepository<JpaRefundRequest, Long> {
    boolean existsByPaymentIdAndStatus(Long paymentId, RefundRequestStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RefundRequest r WHERE r.id = :id")
    Optional<JpaRefundRequest> findForUpdate(@Param("id") Long id);
}
