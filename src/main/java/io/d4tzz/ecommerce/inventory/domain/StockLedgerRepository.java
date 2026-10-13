package io.d4tzz.ecommerce.inventory.domain;

import java.util.List;

/** Append-only: the ledger has no update or delete. */
public interface StockLedgerRepository {
    /** Appends the entries. */
    void saveAll(List<StockLedgerEntry> entries);

    /** Whether the warehouse already has entries caused by this document, e.g. a goods receipt. */
    boolean existsByReference(WarehouseId warehouseId, StockLedgerReference reference);
}
