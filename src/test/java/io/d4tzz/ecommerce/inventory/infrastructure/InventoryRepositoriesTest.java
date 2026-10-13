package io.d4tzz.ecommerce.inventory.infrastructure;

import static org.junit.jupiter.api.Assertions.*;

import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import io.d4tzz.ecommerce.common.Address;
import io.d4tzz.ecommerce.inventory.domain.InventoryItem;
import io.d4tzz.ecommerce.inventory.domain.ReservationLine;
import io.d4tzz.ecommerce.inventory.domain.ReservationStatus;
import io.d4tzz.ecommerce.inventory.domain.StockLedgerEntry;
import io.d4tzz.ecommerce.inventory.domain.StockLedgerReference;
import io.d4tzz.ecommerce.inventory.domain.StockReservation;
import io.d4tzz.ecommerce.inventory.domain.StockReservationService;
import io.d4tzz.ecommerce.inventory.domain.Warehouse;
import io.d4tzz.ecommerce.inventory.domain.WarehouseCode;
import io.d4tzz.ecommerce.inventory.domain.WarehouseId;
import io.d4tzz.ecommerce.inventory.domain.WarehouseStatus;
import io.d4tzz.ecommerce.inventory.infrastructure.persistence.repository.InventoryItemRepositoryImpl;
import io.d4tzz.ecommerce.inventory.infrastructure.persistence.repository.StockLedgerRepositoryImpl;
import io.d4tzz.ecommerce.inventory.infrastructure.persistence.repository.StockReservationRepositoryImpl;
import io.d4tzz.ecommerce.inventory.infrastructure.persistence.repository.WarehouseRepositoryImpl;
import io.d4tzz.ecommerce.support.PostgresContainerTest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({
    WarehouseRepositoryImpl.class,
    InventoryItemRepositoryImpl.class,
    StockReservationRepositoryImpl.class,
    StockLedgerRepositoryImpl.class,
})
class InventoryRepositoriesTest extends PostgresContainerTest {

    // Postgres keeps microseconds; Instant.now() can carry nanoseconds
    private static final Instant T0 = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    private static final Instant T1 = T0.plusSeconds(60);
    private static final Address ADDRESS = Address.of("Ha Noi", "Cau Giay", "Dich Vong", "1 Xuan Thuy");
    private static final VariantId A = VariantId.of(1L);
    private static final VariantId B = VariantId.of(2L);
    private static final VariantId C = VariantId.of(3L);

    @Autowired
    WarehouseRepositoryImpl warehouses;

    @Autowired
    InventoryItemRepositoryImpl items;

    @Autowired
    StockReservationRepositoryImpl reservations;

    @Autowired
    StockLedgerRepositoryImpl ledger;

    @Autowired
    EntityManager em;

    /** Writes pending changes and forgets them, so the next read comes from the database. */
    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    private Warehouse saveWarehouse(String code) {
        return warehouses.save(Warehouse.create(WarehouseCode.of(code), "Warehouse " + code, ADDRESS, c -> false, T0));
    }

    private InventoryItem saveItem(WarehouseId warehouse, VariantId variant, int onHand, int reserved) {
        InventoryItem item = InventoryItem.restore(null, warehouse, variant, onHand, reserved, T0, T0);
        items.saveAll(List.of(item));
        flushAndClear();
        return items.findAllForUpdate(warehouse, List.of(variant)).get(variant);
    }

    private StockReservation saveReservation(long orderId, WarehouseId warehouse, ReservationLine... lines) {
        // restore with no id is how a new reservation looks to the repository; lines are given in variant order
        StockReservation saved = reservations.save(
            StockReservation.restore(null, orderId, warehouse, List.of(lines), ReservationStatus.RESERVED, T0, T0)
        );
        flushAndClear();
        return saved;
    }

    // ---- warehouses ----

    @Test
    void warehouse_roundTripsEveryField() {
        Warehouse saved = saveWarehouse("HN-01");
        flushAndClear();

        Warehouse loaded = warehouses.find(saved.getId()).orElseThrow();

        assertNotNull(loaded.getId());
        assertEquals(WarehouseCode.of("HN-01"), loaded.getCode());
        assertEquals("Warehouse HN-01", loaded.getName());
        assertEquals(ADDRESS, loaded.getAddress());
        assertEquals(WarehouseStatus.ACTIVE, loaded.getStatus());
        assertEquals(T0, loaded.getCreatedAt());
        assertEquals(T0, loaded.getUpdatedAt());
    }

    @Test
    void warehouse_updatesAnExistingRowInsteadOfInsertingANewOne() {
        Warehouse saved = saveWarehouse("HN-01");
        flushAndClear();

        Warehouse loaded = warehouses.find(saved.getId()).orElseThrow();
        loaded.update("Renamed", Address.of("Da Nang", "Hai Chau", "Thach Thang", "2 Bach Dang"), T1);
        loaded.deactivate(T1);
        warehouses.save(loaded);
        flushAndClear();

        Warehouse reloaded = warehouses.find(saved.getId()).orElseThrow();
        assertEquals("Renamed", reloaded.getName());
        assertEquals("Da Nang", reloaded.getAddress().province());
        assertEquals(WarehouseStatus.INACTIVE, reloaded.getStatus());
        assertEquals(T1, reloaded.getUpdatedAt());
        assertEquals(T0, reloaded.getCreatedAt());
        assertEquals(saved.getId(), reloaded.getId());
    }

    @Test
    void warehouse_findReturnsEmptyForAnUnknownId() {
        assertTrue(warehouses.find(WarehouseId.of(999_999L)).isEmpty());
    }

    @Test
    void warehouse_existsByCodeFindsASavedCode() {
        saveWarehouse("HN-01");
        flushAndClear();

        assertTrue(warehouses.existsByCode(WarehouseCode.of("hn-01")));
        assertFalse(warehouses.existsByCode(WarehouseCode.of("HN-02")));
    }

    @Test
    void warehouse_codeMustBeUnique() {
        saveWarehouse("HN-01");

        assertThrows(ConstraintViolationException.class, () -> {
            saveWarehouse("HN-01");
            em.flush();
        });
    }

    // ---- inventory items ----

    @Test
    void items_roundTripQuantities() {
        Warehouse warehouse = saveWarehouse("HN-01");

        InventoryItem loaded = saveItem(warehouse.getId(), A, 10, 4);

        assertNotNull(loaded.getId());
        assertEquals(warehouse.getId(), loaded.getWarehouseId());
        assertEquals(A, loaded.getVariantId());
        assertEquals(10, loaded.getOnHand());
        assertEquals(4, loaded.getReserved());
        assertEquals(T0, loaded.getCreatedAt());
    }

    @Test
    void items_saveAllUpdatesExistingItems() {
        Warehouse warehouse = saveWarehouse("HN-01");
        InventoryItem item = saveItem(warehouse.getId(), A, 10, 0);

        item.receive(5, StockLedgerReference.goodsReceipt("GR-1"), "", T1);
        items.saveAll(List.of(item));
        flushAndClear();

        InventoryItem reloaded = items.findAllForUpdate(warehouse.getId(), List.of(A)).get(A);
        assertEquals(15, reloaded.getOnHand());
        assertEquals(T1, reloaded.getUpdatedAt());
        assertEquals(item.getId(), reloaded.getId());
    }

    @Test
    void items_saveAllInsertsNewAndUpdatesExistingInOneCall() {
        Warehouse warehouse = saveWarehouse("HN-01");
        InventoryItem existing = saveItem(warehouse.getId(), A, 10, 0);
        existing.receive(1, StockLedgerReference.goodsReceipt("GR-1"), "", T1);
        InventoryItem created = InventoryItem.create(warehouse.getId(), B, T0);
        created.receive(3, StockLedgerReference.goodsReceipt("GR-1"), "", T1);

        items.saveAll(List.of(existing, created));
        flushAndClear();

        var loaded = items.findAllForUpdate(warehouse.getId(), List.of(A, B));
        assertEquals(11, loaded.get(A).getOnHand());
        assertEquals(3, loaded.get(B).getOnHand());
    }

    @Test
    void items_findAllForUpdateOnlyReturnsTheWarehouseAndVariantsAsked() {
        Warehouse first = saveWarehouse("HN-01");
        Warehouse second = saveWarehouse("HN-02");
        saveItem(first.getId(), A, 1, 0);
        saveItem(first.getId(), B, 1, 0);
        saveItem(second.getId(), A, 1, 0);

        var loaded = items.findAllForUpdate(first.getId(), List.of(A, C));

        assertEquals(List.of(A), List.copyOf(loaded.keySet()));
        assertEquals(first.getId(), loaded.get(A).getWarehouseId());
    }

    @Test
    void items_findAllForUpdateReturnsNothingForNoVariants() {
        Warehouse warehouse = saveWarehouse("HN-01");

        assertTrue(items.findAllForUpdate(warehouse.getId(), List.of()).isEmpty());
    }

    @Test
    void items_findAllForUpdateReturnsThemInAscendingVariantOrder() {
        Warehouse warehouse = saveWarehouse("HN-01");
        saveItem(warehouse.getId(), C, 1, 0);
        saveItem(warehouse.getId(), A, 1, 0);
        saveItem(warehouse.getId(), B, 1, 0);

        var loaded = items.findAllForUpdate(warehouse.getId(), List.of(C, B, A, B));

        assertEquals(List.of(A, B, C), List.copyOf(loaded.keySet()));
    }

    @Test
    void items_aVariantHasOneItemPerWarehouse() {
        Warehouse warehouse = saveWarehouse("HN-01");
        saveItem(warehouse.getId(), A, 1, 0);

        assertThrows(ConstraintViolationException.class, () -> {
            items.saveAll(List.of(InventoryItem.create(warehouse.getId(), A, T0)));
            em.flush();
        });
    }

    // ---- reservations ----

    @Test
    void reservation_roundTripsStatusAndLines() {
        Warehouse warehouse = saveWarehouse("HN-01");
        StockReservation saved = saveReservation(
            9L,
            warehouse.getId(),
            new ReservationLine(A, 2),
            new ReservationLine(B, 3)
        );

        StockReservation loaded = reservations.findAllForUpdate(9L).get(0);

        assertNotNull(saved.getId());
        assertEquals(saved.getId(), loaded.getId());
        assertEquals(9L, loaded.getOrderId());
        assertEquals(warehouse.getId(), loaded.getWarehouseId());
        assertEquals(ReservationStatus.RESERVED, loaded.getStatus());
        assertEquals(List.of(new ReservationLine(A, 2), new ReservationLine(B, 3)), loaded.getLines());
        assertEquals(T0, loaded.getCreatedAt());
    }

    @Test
    void reservation_saveUpdatesTheStatusOfAnExistingRow() {
        Warehouse warehouse = saveWarehouse("HN-01");
        saveReservation(9L, warehouse.getId(), new ReservationLine(A, 2));
        StockReservation loaded = reservations.findAllForUpdate(9L).get(0);
        InventoryItem item = saveItem(warehouse.getId(), A, 5, 2);

        new StockReservationService().commit(loaded, Map.of(A, item), T1);
        reservations.save(loaded);
        flushAndClear();

        StockReservation reloaded = reservations.findAllForUpdate(9L).get(0);
        assertEquals(ReservationStatus.COMMITTED, reloaded.getStatus());
        assertEquals(T1, reloaded.getUpdatedAt());
        assertEquals(List.of(new ReservationLine(A, 2)), reloaded.getLines());
        assertEquals(1, reservations.findAllForUpdate(9L).size());
    }

    @Test
    void reservation_existsPerOrderAndWarehouse() {
        Warehouse first = saveWarehouse("HN-01");
        Warehouse second = saveWarehouse("HN-02");
        saveReservation(9L, first.getId(), new ReservationLine(A, 1));

        assertTrue(reservations.exists(9L, first.getId()));
        assertFalse(reservations.exists(9L, second.getId()));
        assertFalse(reservations.exists(10L, first.getId()));
    }

    @Test
    void reservation_findAllForUpdateReturnsTheOrdersReservationsByWarehouse() {
        Warehouse first = saveWarehouse("HN-01");
        Warehouse second = saveWarehouse("HN-02");
        saveReservation(9L, second.getId(), new ReservationLine(A, 1));
        saveReservation(9L, first.getId(), new ReservationLine(B, 1));
        saveReservation(10L, first.getId(), new ReservationLine(C, 1));

        List<StockReservation> loaded = reservations.findAllForUpdate(9L);

        assertEquals(
            List.of(first.getId(), second.getId()),
            loaded.stream().map(StockReservation::getWarehouseId).toList()
        );
        assertTrue(reservations.findAllForUpdate(404L).isEmpty());
    }

    @Test
    void reservation_anOrderHasOneReservationPerWarehouse() {
        Warehouse warehouse = saveWarehouse("HN-01");
        saveReservation(9L, warehouse.getId(), new ReservationLine(A, 1));

        assertThrows(ConstraintViolationException.class, () ->
            saveReservation(9L, warehouse.getId(), new ReservationLine(B, 1))
        );
    }

    // ---- ledger ----

    private StockLedgerEntry receiveEntry(WarehouseId warehouse, VariantId variant, String receipt) {
        return InventoryItem.create(warehouse, variant, T0).receive(
            5,
            StockLedgerReference.goodsReceipt(receipt),
            "note",
            T0
        );
    }

    @Test
    void ledger_existsByReferenceIsPerWarehouseAndPerDocument() {
        Warehouse first = saveWarehouse("HN-01");
        Warehouse second = saveWarehouse("HN-02");
        ledger.saveAll(List.of(receiveEntry(first.getId(), A, "GR-1")));
        flushAndClear();

        assertTrue(ledger.existsByReference(first.getId(), StockLedgerReference.goodsReceipt("GR-1")));
        assertFalse(ledger.existsByReference(second.getId(), StockLedgerReference.goodsReceipt("GR-1")));
        assertFalse(ledger.existsByReference(first.getId(), StockLedgerReference.goodsReceipt("GR-2")));
        assertFalse(ledger.existsByReference(first.getId(), StockLedgerReference.order(1L)));
    }

    @Test
    void ledger_existsByReferenceSeesEntriesNotYetFlushed() {
        Warehouse warehouse = saveWarehouse("HN-01");

        ledger.saveAll(List.of(receiveEntry(warehouse.getId(), A, "GR-1")));

        assertTrue(ledger.existsByReference(warehouse.getId(), StockLedgerReference.goodsReceipt("GR-1")));
    }

    @Test
    void ledger_saveAllAppendsEveryEntry() {
        Warehouse warehouse = saveWarehouse("HN-01");

        ledger.saveAll(List.of(receiveEntry(warehouse.getId(), A, "GR-1"), receiveEntry(warehouse.getId(), B, "GR-1")));
        flushAndClear();

        Long count = em
            .createQuery("SELECT COUNT(e) FROM StockLedgerEntry e WHERE e.warehouseId = :w", Long.class)
            .setParameter("w", warehouse.getId().value())
            .getSingleResult();
        assertEquals(2L, count);
    }
}
