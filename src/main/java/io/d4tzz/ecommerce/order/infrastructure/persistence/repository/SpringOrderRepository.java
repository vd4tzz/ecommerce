package io.d4tzz.ecommerce.order.infrastructure.persistence.repository;

import io.d4tzz.ecommerce.order.infrastructure.persistence.entity.JpaOrder;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringOrderRepository extends JpaRepository<JpaOrder, Long> {
    Optional<JpaOrder> findByOrderCode(String orderCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM SalesOrder o WHERE o.id = :id")
    Optional<JpaOrder> findForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM SalesOrder o WHERE o.orderCode = :orderCode")
    Optional<JpaOrder> findForUpdateByOrderCode(@Param("orderCode") String orderCode);

    @Query(
        """
        SELECT o.id FROM SalesOrder o
        WHERE o.status = io.d4tzz.ecommerce.order.domain.OrderStatus.PENDING
          AND o.paymentStatus = io.d4tzz.ecommerce.order.domain.PaymentStatus.UNPAID
          AND o.paymentExpiresAt < :now
        ORDER BY o.id
        """
    )
    List<Long> findPaymentExpiredIds(@Param("now") Instant now);
}
