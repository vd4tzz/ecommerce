package io.d4tzz.ecommerce.inventory.domain;

import io.d4tzz.ecommerce.domain.catalog.product.VariantId;
import java.time.Instant;

/**
 * One line of the stock ledger ("sổ kho"). Append-only: never updated or deleted.
 * Records both quantities after the change so every entry is self-explanatory.
 */
public record StockLedgerEntry(
    StockLedgerEntryId id,
    WarehouseId warehouseId,
    VariantId variantId,
    StockLedgerEntryType type,
    int quantity,
    int onHandAfter,
    int reservedAfter,
    StockLedgerReference reference,
    String note,
    Instant occurredAt
) {
    static StockLedgerEntry record(
        InventoryItem item,
        StockLedgerEntryType type,
        int quantity,
        StockLedgerReference reference,
        String note,
        Instant occurredAt
    ) {
        return new StockLedgerEntry(
            StockLedgerEntryId.generate(),
            item.getWarehouseId(),
            item.getVariantId(),
            type,
            quantity,
            item.getOnHand(),
            item.getReserved(),
            reference,
            note,
            occurredAt
        );
    }
}
