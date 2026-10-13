package io.d4tzz.ecommerce.inventory.domain;

import static org.junit.jupiter.api.Assertions.*;

import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class InventoryItemTest {

    private static final WarehouseId WAREHOUSE = WarehouseId.of(1L);
    private static final VariantId VARIANT = VariantId.of(10L);
    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = T0.plusSeconds(60);
    private static final StockLedgerReference ORDER = StockLedgerReference.order(5L);
    private static final StockLedgerReference RECEIPT = StockLedgerReference.goodsReceipt("GR-1");

    private static InventoryItem item(int onHand, int reserved) {
        return InventoryItem.restore(InventoryItemId.of(1L), WAREHOUSE, VARIANT, onHand, reserved, T0, T0);
    }

    // ---- creating ----

    @Test
    void create_startsWithoutStockAndWithoutAnId() {
        InventoryItem item = InventoryItem.create(WAREHOUSE, VARIANT, T0);

        assertNull(item.getId());
        assertEquals(0, item.getOnHand());
        assertEquals(0, item.getReserved());
        assertEquals(0, item.available());
        assertEquals(T0, item.getCreatedAt());
        assertEquals(T0, item.getUpdatedAt());
    }

    @Test
    void restore_rejectsMoreReservedThanOnHand() {
        assertThrows(InvalidArgumentException.class, () -> item(2, 3));
    }

    @Test
    void restore_rejectsNegativeReserved() {
        assertThrows(InvalidArgumentException.class, () -> item(2, -1));
    }

    // ---- receive ----

    @Test
    void receive_addsToOnHandAndRecordsTheLedgerEntry() {
        InventoryItem item = item(5, 2);

        StockLedgerEntry entry = item.receive(10, RECEIPT, "pallet 3", T1);

        assertEquals(15, item.getOnHand());
        assertEquals(2, item.getReserved());
        assertEquals(13, item.available());
        assertEquals(T1, item.getUpdatedAt());
        assertEquals(StockLedgerEntryType.RECEIVE, entry.type());
        assertEquals(10, entry.quantity());
        assertEquals(15, entry.onHandAfter());
        assertEquals(2, entry.reservedAfter());
        assertEquals(RECEIPT, entry.reference());
        assertEquals("pallet 3", entry.note());
        assertEquals(WAREHOUSE, entry.warehouseId());
        assertEquals(VARIANT, entry.variantId());
        assertEquals(T1, entry.occurredAt());
        assertNotNull(entry.id());
    }

    @Test
    void receive_rejectsZeroAndNegativeQuantities() {
        InventoryItem item = item(5, 0);

        assertThrows(InvalidArgumentException.class, () -> item.receive(0, RECEIPT, "", T1));
        assertThrows(InvalidArgumentException.class, () -> item.receive(-1, RECEIPT, "", T1));
        assertEquals(5, item.getOnHand());
    }

    // ---- reserve ----

    @Test
    void reserve_holdsUnitsWithoutChangingOnHand() {
        InventoryItem item = item(5, 1);

        StockLedgerEntry entry = item.reserve(4, ORDER, T1);

        assertEquals(5, item.getOnHand());
        assertEquals(5, item.getReserved());
        assertEquals(0, item.available());
        assertEquals(StockLedgerEntryType.RESERVE, entry.type());
        assertEquals(4, entry.quantity());
        assertEquals(5, entry.onHandAfter());
        assertEquals(5, entry.reservedAfter());
        assertEquals(ORDER, entry.reference());
    }

    @Test
    void reserve_failsWhenFewerUnitsAreAvailable() {
        InventoryItem item = item(5, 3);

        assertThrows(OutOfStockException.class, () -> item.reserve(3, ORDER, T1));
        assertEquals(3, item.getReserved());
        assertEquals(T0, item.getUpdatedAt());
    }

    @Test
    void reserve_rejectsZeroQuantity() {
        assertThrows(InvalidArgumentException.class, () -> item(5, 0).reserve(0, ORDER, T1));
    }

    // ---- release ----

    @Test
    void release_givesHeldUnitsBackToAvailable() {
        InventoryItem item = item(5, 4);

        StockLedgerEntry entry = item.release(3, ORDER, T1);

        assertEquals(5, item.getOnHand());
        assertEquals(1, item.getReserved());
        assertEquals(4, item.available());
        assertEquals(StockLedgerEntryType.RELEASE, entry.type());
        assertEquals(1, entry.reservedAfter());
    }

    @Test
    void release_failsWhenFewerUnitsAreReserved() {
        InventoryItem item = item(5, 2);

        assertThrows(InvalidOperationException.class, () -> item.release(3, ORDER, T1));
        assertEquals(2, item.getReserved());
    }

    // ---- commit ----

    @Test
    void commit_removesTheUnitsFromBothOnHandAndReserved() {
        InventoryItem item = item(5, 4);

        StockLedgerEntry entry = item.commit(3, ORDER, T1);

        assertEquals(2, item.getOnHand());
        assertEquals(1, item.getReserved());
        assertEquals(1, item.available());
        assertEquals(StockLedgerEntryType.COMMIT, entry.type());
        assertEquals(2, entry.onHandAfter());
        assertEquals(1, entry.reservedAfter());
    }

    @Test
    void commit_failsWhenFewerUnitsAreReserved() {
        InventoryItem item = item(5, 2);

        assertThrows(InvalidOperationException.class, () -> item.commit(3, ORDER, T1));
        assertEquals(5, item.getOnHand());
        assertEquals(2, item.getReserved());
    }

    // ---- restock ----

    @Test
    void restock_addsToOnHandOnly() {
        InventoryItem item = item(2, 1);

        StockLedgerEntry entry = item.restock(3, ORDER, T1);

        assertEquals(5, item.getOnHand());
        assertEquals(1, item.getReserved());
        assertEquals(StockLedgerEntryType.RESTOCK, entry.type());
        assertEquals(5, entry.onHandAfter());
    }

    @Test
    void restock_rejectsZeroQuantity() {
        assertThrows(InvalidArgumentException.class, () -> item(2, 0).restock(0, ORDER, T1));
    }

    // ---- ledger note ----

    @Test
    void changesOtherThanReceiveHaveAnEmptyNote() {
        InventoryItem item = item(5, 0);

        assertEquals("", item.reserve(1, ORDER, T1).note());
        assertEquals("", item.release(1, ORDER, T1).note());
    }
}
