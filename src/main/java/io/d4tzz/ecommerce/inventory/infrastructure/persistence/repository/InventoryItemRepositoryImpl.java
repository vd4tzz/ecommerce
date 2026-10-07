package io.d4tzz.ecommerce.inventory.infrastructure.persistence.repository;

import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import io.d4tzz.ecommerce.inventory.domain.InventoryItem;
import io.d4tzz.ecommerce.inventory.domain.InventoryItemId;
import io.d4tzz.ecommerce.inventory.domain.InventoryItemRepository;
import io.d4tzz.ecommerce.inventory.domain.WarehouseId;
import io.d4tzz.ecommerce.inventory.infrastructure.persistence.entity.JpaInventoryItem;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class InventoryItemRepositoryImpl implements InventoryItemRepository {

    private final SpringInventoryItemRepository springRepo;

    public InventoryItemRepositoryImpl(SpringInventoryItemRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public Map<VariantId, InventoryItem> findAllForUpdate(WarehouseId warehouseId, Collection<VariantId> variantIds) {
        if (variantIds.isEmpty()) {
            return Map.of();
        }

        List<Long> ids = variantIds.stream().map(VariantId::value).distinct().sorted().toList();

        Map<VariantId, InventoryItem> items = new LinkedHashMap<>();
        for (JpaInventoryItem jpa : springRepo.findAllForUpdate(warehouseId.value(), ids)) {
            InventoryItem item = toDomain(jpa);
            items.put(item.getVariantId(), item);
        }
        return items;
    }

    @Override
    public void saveAll(Collection<InventoryItem> items) {
        List<JpaInventoryItem> entities = new ArrayList<>(items.size());
        for (InventoryItem item : items) {
            JpaInventoryItem jpa;
            if (item.getId() == null) {
                jpa = new JpaInventoryItem();
                jpa.setWarehouseId(item.getWarehouseId().value());
                jpa.setVariantId(item.getVariantId().value());
                jpa.setCreatedAt(item.getCreatedAt());
            } else {
                // Already managed (and locked) by findAllForUpdate in the same transaction: no extra query
                jpa = springRepo.findById(item.getId().value()).orElseThrow();
            }
            jpa.setOnHand(item.getOnHand());
            jpa.setReserved(item.getReserved());
            jpa.setUpdatedAt(item.getUpdatedAt());
            entities.add(jpa);
        }
        springRepo.saveAll(entities);
    }

    private InventoryItem toDomain(JpaInventoryItem jpa) {
        return InventoryItem.restore(
            InventoryItemId.of(jpa.getId()),
            WarehouseId.of(jpa.getWarehouseId()),
            VariantId.of(jpa.getVariantId()),
            jpa.getOnHand(),
            jpa.getReserved(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt()
        );
    }
}
