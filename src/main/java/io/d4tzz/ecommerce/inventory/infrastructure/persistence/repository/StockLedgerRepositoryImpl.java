package io.d4tzz.ecommerce.inventory.infrastructure.persistence.repository;

import io.d4tzz.ecommerce.inventory.domain.StockLedgerEntry;
import io.d4tzz.ecommerce.inventory.domain.StockLedgerReference;
import io.d4tzz.ecommerce.inventory.domain.StockLedgerRepository;
import io.d4tzz.ecommerce.inventory.domain.WarehouseId;
import io.d4tzz.ecommerce.inventory.infrastructure.persistence.entity.JpaStockLedgerEntry;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class StockLedgerRepositoryImpl implements StockLedgerRepository {

    private final EntityManager entityManager;

    public StockLedgerRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void saveAll(List<StockLedgerEntry> entries) {
        // persist (not merge): ids are generated in the domain and entries are always new
        for (StockLedgerEntry entry : entries) {
            entityManager.persist(
                new JpaStockLedgerEntry(
                    entry.id().value(),
                    entry.warehouseId().value(),
                    entry.variantId().value(),
                    entry.type(),
                    entry.quantity(),
                    entry.onHandAfter(),
                    entry.reservedAfter(),
                    entry.reference().type(),
                    entry.reference().value(),
                    entry.note(),
                    entry.occurredAt()
                )
            );
        }
    }

    @Override
    public boolean existsByReference(WarehouseId warehouseId, StockLedgerReference reference) {
        return !entityManager
            .createQuery(
                """
                SELECT e.id FROM StockLedgerEntry e
                WHERE e.warehouseId = :warehouseId
                  AND e.referenceType = :referenceType
                  AND e.referenceValue = :referenceValue
                """,
                UUID.class
            )
            .setParameter("warehouseId", warehouseId.value())
            .setParameter("referenceType", reference.type())
            .setParameter("referenceValue", reference.value())
            .setMaxResults(1)
            .getResultList()
            .isEmpty();
    }
}
