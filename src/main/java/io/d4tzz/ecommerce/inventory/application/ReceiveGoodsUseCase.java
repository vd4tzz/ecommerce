package io.d4tzz.ecommerce.inventory.application;

import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import io.d4tzz.ecommerce.catalog.published.VariantExistenceChecker;
import io.d4tzz.ecommerce.common.exception.ConflictException;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.inventory.application.command.ReceiveGoodsCommand;
import io.d4tzz.ecommerce.inventory.domain.InventoryItem;
import io.d4tzz.ecommerce.inventory.domain.InventoryItemRepository;
import io.d4tzz.ecommerce.inventory.domain.StockLedgerEntry;
import io.d4tzz.ecommerce.inventory.domain.StockLedgerReference;
import io.d4tzz.ecommerce.inventory.domain.StockLedgerRepository;
import io.d4tzz.ecommerce.inventory.domain.Warehouse;
import io.d4tzz.ecommerce.inventory.domain.WarehouseId;
import io.d4tzz.ecommerce.inventory.domain.WarehouseRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReceiveGoodsUseCase {

    private final WarehouseRepository warehouseRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final StockLedgerRepository stockLedgerRepository;
    private final VariantExistenceChecker variantExistenceChecker;

    public ReceiveGoodsUseCase(
        WarehouseRepository warehouseRepository,
        InventoryItemRepository inventoryItemRepository,
        StockLedgerRepository stockLedgerRepository,
        VariantExistenceChecker variantExistenceChecker
    ) {
        this.warehouseRepository = warehouseRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.stockLedgerRepository = stockLedgerRepository;
        this.variantExistenceChecker = variantExistenceChecker;
    }

    /**
     * Puts goods into an active warehouse and records them in the ledger under the receipt code. A receipt code can be
     * used once per warehouse, so a repeated request does not count the goods twice. A variant the warehouse has not
     * stocked before gets its item here. Returns the ids of the ledger entries recorded for this receipt.
     *
     * @throws NotFoundException         if the warehouse or a variant does not exist
     * @throws InvalidOperationException if the warehouse is inactive
     * @throws ConflictException         if the receipt code was already received into the warehouse
     */
    public List<UUID> execute(ReceiveGoodsCommand command) {
        WarehouseId warehouseId = WarehouseId.of(command.warehouseId());
        Warehouse warehouse = warehouseRepository
            .find(warehouseId)
            .orElseThrow(() -> new NotFoundException("Warehouse not found with id: " + command.warehouseId()));
        warehouse.ensureActive();

        Map<VariantId, Integer> quantities = new HashMap<>();
        for (ReceiveGoodsCommand.Item item : command.items()) {
            quantities.merge(VariantId.of(item.variantId()), item.quantity(), Integer::sum);
        }

        // ensure variants must exist in catalog
        Set<VariantId> missing = variantExistenceChecker.findMissing(quantities.keySet());
        if (!missing.isEmpty()) {
            throw new NotFoundException("Variants not found: " + missing);
        }

        Instant now = Instant.now();
        StockLedgerReference reference = StockLedgerReference.goodsReceipt(command.receiptCode());

        Map<VariantId, InventoryItem> items = new HashMap<>(
            inventoryItemRepository.findAllForUpdate(warehouseId, quantities.keySet())
        );

        // The same receipt must not be counted twice, e.g. when the admin clicks twice or resends after a timeout.
        // This check comes after the locks above: if two copies of this request run at the same time, the second
        // one waits until the first is saved, and then finds it here.
        // If the warehouse has none of these items yet, there is nothing to lock. The second copy then fails
        // when it tries to create an item the first one already created.
        if (stockLedgerRepository.existsByReference(warehouseId, reference)) {
            throw new ConflictException(
                "Receipt " + command.receiptCode() + " was already received into warehouse " + warehouse.getCode()
            );
        }

        List<StockLedgerEntry> entries = new ArrayList<>(quantities.size());
        quantities.forEach((variantId, quantity) -> {
            // if iventory item not exist (absent in map) create new one.
            InventoryItem item = items.computeIfAbsent(variantId, id -> InventoryItem.create(warehouseId, id, now));
            StockLedgerEntry stockLedgerEntry = item.receive(quantity, reference, command.note(), now);
            entries.add(stockLedgerEntry);
        });

        inventoryItemRepository.saveAll(items.values());
        stockLedgerRepository.saveAll(entries);
        return entries
            .stream()
            .map(entry -> entry.id().value())
            .toList();
    }
}
