package io.d4tzz.ecommerce.inventory.domain;

import static org.junit.jupiter.api.Assertions.*;

import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import io.d4tzz.ecommerce.common.Address;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StockReservationServiceTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = T0.plusSeconds(60);
    private static final WarehouseId WAREHOUSE_ID = WarehouseId.of(1L);
    private static final VariantId A = VariantId.of(1L);
    private static final VariantId B = VariantId.of(2L);

    private final StockReservationService service = new StockReservationService();

    private static Warehouse warehouse(WarehouseStatus status) {
        return Warehouse.restore(
            WAREHOUSE_ID,
            WarehouseCode.of("HN-01"),
            "Hanoi",
            Address.of("Ha Noi", "Cau Giay", "Dich Vong", "1 Xuan Thuy"),
            status,
            T0,
            T0
        );
    }

    private static InventoryItem item(VariantId variant, int onHand, int reserved) {
        return InventoryItem.restore(InventoryItemId.of(variant.value()), WAREHOUSE_ID, variant, onHand, reserved, T0, T0);
    }

    private static Map<VariantId, InventoryItem> items(InventoryItem... items) {
        Map<VariantId, InventoryItem> map = new HashMap<>();
        for (InventoryItem item : items) {
            map.put(item.getVariantId(), item);
        }
        return map;
    }

    private static ReservationLine line(VariantId variant, int quantity) {
        return new ReservationLine(variant, quantity);
    }

    private StockReservationService.ReservationResult reserve(Map<VariantId, InventoryItem> items) {
        return service.reserve(9L, warehouse(WarehouseStatus.ACTIVE), List.of(line(A, 2), line(B, 1)), items, T1);
    }

    // ---- reserve ----

    @Test
    void reserve_holdsEveryLineAndReturnsOneEntryPerLine() {
        InventoryItem a = item(A, 5, 0);
        InventoryItem b = item(B, 5, 1);

        var result = reserve(items(a, b));

        assertEquals(2, a.getReserved());
        assertEquals(2, b.getReserved());
        assertEquals(ReservationStatus.RESERVED, result.reservation().getStatus());
        assertEquals(9L, result.reservation().getOrderId());
        assertEquals(WAREHOUSE_ID, result.reservation().getWarehouseId());
        assertEquals(2, result.entries().size());
        assertTrue(result.entries().stream().allMatch(e -> e.type() == StockLedgerEntryType.RESERVE));
        assertTrue(result.entries().stream().allMatch(e -> e.reference().equals(StockLedgerReference.order(9L))));
    }

    @Test
    void reserve_failsWhenTheWarehouseIsInactive() {
        InventoryItem a = item(A, 5, 0);

        assertThrows(
            InvalidOperationException.class,
            () -> service.reserve(9L, warehouse(WarehouseStatus.INACTIVE), List.of(line(A, 1)), items(a), T1)
        );
        assertEquals(0, a.getReserved());
    }

    @Test
    void reserve_failsWhenAVariantHasNoItemInTheWarehouse() {
        var thrown = assertThrows(InvalidOperationException.class, () -> reserve(items(item(A, 5, 0))));

        assertTrue(thrown.getMessage().contains("out of stock"));
    }

    @Test
    void reserve_failsWhenAVariantHasTooLittleAvailable() {
        assertThrows(InvalidOperationException.class, () -> reserve(items(item(A, 5, 0), item(B, 1, 1))));
    }

    @Test
    void reserve_ignoresItemsTheLinesDoNotNeed() {
        InventoryItem other = item(VariantId.of(3L), 5, 0);

        reserve(items(item(A, 5, 0), item(B, 5, 0), other));

        assertEquals(0, other.getReserved());
    }

    // ---- release / commit / restock ----

    private StockReservation reservation(ReservationStatus status) {
        return StockReservation.restore(
            StockReservationId.of(1L),
            9L,
            WAREHOUSE_ID,
            List.of(line(A, 2), line(B, 1)),
            status,
            T0,
            T0
        );
    }

    @Test
    void release_updatesTheItemsAndReturnsRELEASEEntries() {
        InventoryItem a = item(A, 5, 2);
        InventoryItem b = item(B, 5, 1);
        StockReservation reservation = reservation(ReservationStatus.RESERVED);

        List<StockLedgerEntry> entries = service.release(reservation, items(a, b), T1);

        assertEquals(ReservationStatus.RELEASED, reservation.getStatus());
        assertEquals(0, a.getReserved());
        assertEquals(0, b.getReserved());
        assertEquals(2, entries.size());
        assertTrue(entries.stream().allMatch(e -> e.type() == StockLedgerEntryType.RELEASE));
    }

    @Test
    void commit_takesTheUnitsOutOfOnHand() {
        InventoryItem a = item(A, 5, 2);
        InventoryItem b = item(B, 5, 1);
        StockReservation reservation = reservation(ReservationStatus.RESERVED);

        List<StockLedgerEntry> entries = service.commit(reservation, items(a, b), T1);

        assertEquals(ReservationStatus.COMMITTED, reservation.getStatus());
        assertEquals(3, a.getOnHand());
        assertEquals(4, b.getOnHand());
        assertEquals(0, a.getReserved());
        assertTrue(entries.stream().allMatch(e -> e.type() == StockLedgerEntryType.COMMIT));
    }

    @Test
    void restock_putsTheUnitsBackOnHand() {
        InventoryItem a = item(A, 3, 0);
        InventoryItem b = item(B, 4, 0);
        StockReservation reservation = reservation(ReservationStatus.COMMITTED);

        List<StockLedgerEntry> entries = service.restock(reservation, items(a, b), T1);

        assertEquals(ReservationStatus.RESTOCKED, reservation.getStatus());
        assertEquals(5, a.getOnHand());
        assertEquals(5, b.getOnHand());
        assertTrue(entries.stream().allMatch(e -> e.type() == StockLedgerEntryType.RESTOCK));
    }

    @Test
    void release_commit_restock_doNothingWhenAlreadyInTheTargetStatus() {
        InventoryItem a = item(A, 5, 0);
        InventoryItem b = item(B, 5, 0);

        assertTrue(service.release(reservation(ReservationStatus.RELEASED), items(a, b), T1).isEmpty());
        assertTrue(service.commit(reservation(ReservationStatus.COMMITTED), items(a, b), T1).isEmpty());
        assertTrue(service.restock(reservation(ReservationStatus.RESTOCKED), items(a, b), T1).isEmpty());

        assertEquals(5, a.getOnHand());
        assertEquals(0, a.getReserved());
    }

    @Test
    void release_ofACommittedReservationIsRejected() {
        assertThrows(
            InvalidOperationException.class,
            () -> service.release(reservation(ReservationStatus.COMMITTED), items(item(A, 5, 2), item(B, 5, 1)), T1)
        );
    }

    @Test
    void release_failsWhenAnItemIsMissing() {
        assertThrows(
            IllegalStateException.class,
            () -> service.release(reservation(ReservationStatus.RESERVED), items(item(A, 5, 2)), T1)
        );
    }
}
