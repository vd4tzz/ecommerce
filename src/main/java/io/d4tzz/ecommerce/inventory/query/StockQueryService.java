package io.d4tzz.ecommerce.inventory.query;

import io.d4tzz.ecommerce.query.PageResponse;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Read side of stock: levels, ledger and reservations. */
public interface StockQueryService {
    /** Stock items, by product name, SKU, then warehouse code. */
    PageResponse<StockItemView> getStock(StockSearchCriteria criteria);

    /** Sellable stock of the given variants per active warehouse. */
    List<VariantAvailabilityView> getAvailability(Collection<Long> variantIds);

    /** Ledger entries, newest first. */
    PageResponse<StockLedgerEntryView> getLedger(StockLedgerSearchCriteria criteria);

    /** The given ledger entries, by warehouse then variant. */
    List<StockLedgerEntryView> getLedgerEntries(Collection<UUID> ids);

    /** The reservations of an order, one per warehouse, in ascending warehouse id order. */
    List<StockReservationView> getReservationsByOrder(Long orderId);
}
