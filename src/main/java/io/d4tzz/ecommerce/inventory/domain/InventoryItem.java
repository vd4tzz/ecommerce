package io.d4tzz.ecommerce.inventory.domain;

import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import java.time.Instant;

/**
 * Stock of one variant in one warehouse.
 *
 * <p>Invariant: {@code 0 <= reserved <= onHand}.
 *
 * <p>Only {@link #receive} is public. Reserve/release/commit/restock are package-private so they can only be
 * driven through {@link StockReservationService}, which keeps them in sync with {@link StockReservation}.
 * Every change returns the {@link StockLedgerEntry} describing it.
 */
public class InventoryItem {

    private final InventoryItemId id;
    private final WarehouseId warehouseId;
    private final VariantId variantId;
    private int onHand;
    private int reserved;
    private final Instant createdAt;
    private Instant updatedAt;

    private InventoryItem(
        InventoryItemId id,
        WarehouseId warehouseId,
        VariantId variantId,
        int onHand,
        int reserved,
        Instant createdAt,
        Instant updatedAt
    ) {
        if (reserved < 0 || reserved > onHand) {
            throw new InvalidArgumentException("Invalid stock quantities: onHand=" + onHand + ", reserved=" + reserved);
        }
        this.id = id;
        this.warehouseId = warehouseId;
        this.variantId = variantId;
        this.onHand = onHand;
        this.reserved = reserved;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static InventoryItem create(WarehouseId warehouseId, VariantId variantId, Instant now) {
        return new InventoryItem(null, warehouseId, variantId, 0, 0, now, now);
    }

    public static InventoryItem restore(
        InventoryItemId id,
        WarehouseId warehouseId,
        VariantId variantId,
        int onHand,
        int reserved,
        Instant createdAt,
        Instant updatedAt
    ) {
        return new InventoryItem(id, warehouseId, variantId, onHand, reserved, createdAt, updatedAt);
    }

    public StockLedgerEntry receive(int quantity, StockLedgerReference reference, String note, Instant now) {
        requirePositive(quantity);
        this.onHand += quantity;
        return recordChange(StockLedgerEntryType.RECEIVE, quantity, reference, note, now);
    }

    StockLedgerEntry reserve(int quantity, StockLedgerReference reference, Instant now) {
        requirePositive(quantity);
        if (available() < quantity) {
            throw new InvalidOperationException(
                "Insufficient stock for variant " + variantId + ": available " + available() + ", requested " + quantity
            );
        }
        this.reserved += quantity;
        return recordChange(StockLedgerEntryType.RESERVE, quantity, reference, "", now);
    }

    StockLedgerEntry release(int quantity, StockLedgerReference reference, Instant now) {
        requirePositive(quantity);
        requireReserved(quantity);
        this.reserved -= quantity;
        return recordChange(StockLedgerEntryType.RELEASE, quantity, reference, "", now);
    }

    StockLedgerEntry commit(int quantity, StockLedgerReference reference, Instant now) {
        requirePositive(quantity);
        requireReserved(quantity);
        this.onHand -= quantity;
        this.reserved -= quantity;
        return recordChange(StockLedgerEntryType.COMMIT, quantity, reference, "", now);
    }

    StockLedgerEntry restock(int quantity, StockLedgerReference reference, Instant now) {
        requirePositive(quantity);
        this.onHand += quantity;
        return recordChange(StockLedgerEntryType.RESTOCK, quantity, reference, "", now);
    }

    public int available() {
        return onHand - reserved;
    }

    private StockLedgerEntry recordChange(
        StockLedgerEntryType type,
        int quantity,
        StockLedgerReference reference,
        String note,
        Instant now
    ) {
        this.updatedAt = now;
        return StockLedgerEntry.record(this, type, quantity, reference, note, now);
    }

    private static void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new InvalidArgumentException("Quantity must be greater than zero");
        }
    }

    private void requireReserved(int quantity) {
        if (reserved < quantity) {
            throw new InvalidOperationException(
                "Variant " + variantId + " has only " + reserved + " reserved, cannot take " + quantity
            );
        }
    }

    public InventoryItemId getId() {
        return id;
    }

    public WarehouseId getWarehouseId() {
        return warehouseId;
    }

    public VariantId getVariantId() {
        return variantId;
    }

    public int getOnHand() {
        return onHand;
    }

    public int getReserved() {
        return reserved;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
