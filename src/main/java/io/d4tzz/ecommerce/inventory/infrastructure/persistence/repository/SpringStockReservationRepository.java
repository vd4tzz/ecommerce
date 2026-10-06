package io.d4tzz.ecommerce.inventory.infrastructure.persistence.repository;

import io.d4tzz.ecommerce.inventory.infrastructure.persistence.entity.JpaStockReservation;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringStockReservationRepository extends JpaRepository<JpaStockReservation, Long> {
    boolean existsByOrderIdAndWarehouseId(Long orderId, Long warehouseId);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM StockReservation r WHERE r.orderId = :orderId ORDER BY r.warehouseId")
    List<JpaStockReservation> findAllForUpdate(@Param("orderId") Long orderId);
}
