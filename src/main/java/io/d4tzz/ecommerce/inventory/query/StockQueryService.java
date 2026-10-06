package io.d4tzz.ecommerce.inventory.query;

import io.d4tzz.ecommerce.query.PageResponse;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface StockQueryService {
    PageResponse<StockItemView> getStock(StockSearchCriteria criteria);

    /** Sellable stock of the given variants per active warehouse. */
    List<VariantAvailabilityView> getAvailability(Collection<Long> variantIds);

    PageResponse<StockLedgerEntryView> getLedger(StockLedgerSearchCriteria criteria);

    List<StockLedgerEntryView> getLedgerEntries(Collection<UUID> ids);

    List<StockReservationView> getReservationsByOrder(Long orderId);
}
