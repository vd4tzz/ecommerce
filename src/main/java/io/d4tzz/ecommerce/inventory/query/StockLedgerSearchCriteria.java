package io.d4tzz.ecommerce.inventory.query;

import java.time.Instant;

/** Every filter is optional. Entries from {@code from} (included) to {@code to} (excluded). */
public record StockLedgerSearchCriteria(
    Long warehouseId,
    Long variantId,
    String type,
    String referenceType,
    String referenceValue,
    Instant from,
    Instant to,
    Integer page,
    Integer size
) {
    public int getPage() {
        return page == null || page < 0 ? 0 : page;
    }

    public int getSize() {
        return size == null || size <= 0 ? 20 : size;
    }

    public int getOffset() {
        return getPage() * getSize();
    }
}
