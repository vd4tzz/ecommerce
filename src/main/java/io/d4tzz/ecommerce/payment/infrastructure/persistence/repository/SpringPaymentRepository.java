package io.d4tzz.ecommerce.payment.infrastructure.persistence.repository;

import io.d4tzz.ecommerce.payment.domain.PaymentMethod;
import io.d4tzz.ecommerce.payment.infrastructure.persistence.entity.JpaPayment;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringPaymentRepository extends JpaRepository<JpaPayment, Long> {
    Optional<JpaPayment> findByMethodAndShopAccountAndTransactionNo(
        PaymentMethod method,
        String shopAccount,
        String transactionNo
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :id")
    Optional<JpaPayment> findForUpdate(@Param("id") Long id);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.refundOf = :paymentId")
    BigDecimal sumRefunds(@Param("paymentId") Long paymentId);
}
