package io.d4tzz.ecommerce.inventory.domain;

import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.domain.catalog.product.VariantId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Domain service coordinating {@link StockReservation} and {@link InventoryItem}: every reservation status change
 * is applied to the items in the same step, so {@code item.reserved} always equals the sum of the RESERVED lines.
 *
 * <p>Callers must pass items that are already locked for update.
 */
public class StockReservationService {

    public record ReservationResult(StockReservation reservation, List<StockLedgerEntry> entries) {}

    private enum ReservationAction {
        RELEASE,
        COMMIT,
        RESTOCK,
    }

    // used to reserve for an order
    public ReservationResult reserve(
        Long orderId,
        Warehouse warehouse,
        List<ReservationLine> lines,
        Map<VariantId, InventoryItem> lockedItems,
        Instant now
    ) {
        warehouse.ensureActive();
        StockReservation reservation = StockReservation.create(orderId, warehouse.getId(), lines, now);
        StockLedgerReference reference = StockLedgerReference.order(orderId);
        List<StockLedgerEntry> entries = new ArrayList<>(reservation.getLines().size());
        for (ReservationLine line : reservation.getLines()) {
            InventoryItem item = lockedItems.get(line.variantId());
            if (item == null) {
                throw new InvalidOperationException(
                    "Variant " + line.variantId() + " is out of stock in warehouse " + warehouse.getCode()
                );
            }
            entries.add(item.reserve(line.quantity(), reference, now));
        }
        return new ReservationResult(reservation, entries);
    }

    /** Returns no entries when the reservation was already released. */
    public List<StockLedgerEntry> release(
        StockReservation reservation,
        Map<VariantId, InventoryItem> lockedItems,
        Instant now
    ) {
        if (!reservation.release(now)) {
            return List.of();
        }
        return applyToItems(reservation, lockedItems, ReservationAction.RELEASE, now);
    }

    /** Returns no entries when the reservation was already committed. */
    public List<StockLedgerEntry> commit(
        StockReservation reservation,
        Map<VariantId, InventoryItem> lockedItems,
        Instant now
    ) {
        if (!reservation.commit(now)) {
            return List.of();
        }
        return applyToItems(reservation, lockedItems, ReservationAction.COMMIT, now);
    }

    /** Returns no entries when the reservation was already restocked. */
    public List<StockLedgerEntry> restock(
        StockReservation reservation,
        Map<VariantId, InventoryItem> lockedItems,
        Instant now
    ) {
        if (!reservation.restock(now)) {
            return List.of();
        }
        return applyToItems(reservation, lockedItems, ReservationAction.RESTOCK, now);
    }

    private List<StockLedgerEntry> applyToItems(
        StockReservation reservation,
        Map<VariantId, InventoryItem> lockedItems,
        ReservationAction action,
        Instant now
    ) {
        StockLedgerReference reference = StockLedgerReference.order(reservation.getOrderId());
        List<StockLedgerEntry> entries = new ArrayList<>(reservation.getLines().size());
        for (ReservationLine line : reservation.getLines()) {
            InventoryItem item = lockedItems.get(line.variantId());
            if (item == null) {
                throw new IllegalStateException(
                    "Inventory item missing for reserved variant " +
                        line.variantId() +
                        " in warehouse " +
                        reservation.getWarehouseId()
                );
            }
            entries.add(
                switch (action) {
                    case RELEASE -> item.release(line.quantity(), reference, now);
                    case COMMIT -> item.commit(line.quantity(), reference, now);
                    case RESTOCK -> item.restock(line.quantity(), reference, now);
                }
            );
        }
        return entries;
    }
}
