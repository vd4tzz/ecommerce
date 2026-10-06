package io.d4tzz.ecommerce.inventory.infrastructure.persistence.repository;

import io.d4tzz.ecommerce.inventory.infrastructure.persistence.entity.JpaInventoryItem;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringInventoryItemRepository extends JpaRepository<JpaInventoryItem, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        SELECT i FROM InventoryItem i
        WHERE i.warehouseId = :warehouseId AND i.variantId IN :variantIds
        ORDER BY i.variantId
        """
    )
    List<JpaInventoryItem> findAllForUpdate(
        @Param("warehouseId") Long warehouseId,
        @Param("variantIds") Collection<Long> variantIds
    );
}
