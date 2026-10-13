package io.d4tzz.ecommerce.inventory.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import io.d4tzz.ecommerce.catalog.published.VariantExistenceChecker;
import io.d4tzz.ecommerce.common.Address;
import io.d4tzz.ecommerce.common.exception.ConflictException;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.inventory.application.command.AllocateStockCommand;
import io.d4tzz.ecommerce.inventory.application.command.ChangeWarehouseStatusCommand;
import io.d4tzz.ecommerce.inventory.application.command.CommitStockCommand;
import io.d4tzz.ecommerce.inventory.application.command.CreateWarehouseCommand;
import io.d4tzz.ecommerce.inventory.application.command.ReceiveGoodsCommand;
import io.d4tzz.ecommerce.inventory.application.command.ReleaseStockCommand;
import io.d4tzz.ecommerce.inventory.application.command.ReserveStockCommand;
import io.d4tzz.ecommerce.inventory.application.command.RestockReturnedCommand;
import io.d4tzz.ecommerce.inventory.application.command.UpdateWarehouseCommand;
import io.d4tzz.ecommerce.inventory.application.result.StockAllocation;
import io.d4tzz.ecommerce.inventory.domain.InventoryItem;
import io.d4tzz.ecommerce.inventory.domain.InventoryItemId;
import io.d4tzz.ecommerce.inventory.domain.InventoryItemRepository;
import io.d4tzz.ecommerce.inventory.domain.OutOfStockException;
import io.d4tzz.ecommerce.inventory.domain.ReservationLine;
import io.d4tzz.ecommerce.inventory.domain.ReservationStatus;
import io.d4tzz.ecommerce.inventory.domain.StockAllocator;
import io.d4tzz.ecommerce.inventory.domain.StockLedgerEntry;
import io.d4tzz.ecommerce.inventory.domain.StockLedgerEntryType;
import io.d4tzz.ecommerce.inventory.domain.StockLedgerReference;
import io.d4tzz.ecommerce.inventory.domain.StockLedgerRepository;
import io.d4tzz.ecommerce.inventory.domain.StockReservation;
import io.d4tzz.ecommerce.inventory.domain.StockReservationId;
import io.d4tzz.ecommerce.inventory.domain.StockReservationRepository;
import io.d4tzz.ecommerce.inventory.domain.StockReservationService;
import io.d4tzz.ecommerce.inventory.domain.Warehouse;
import io.d4tzz.ecommerce.inventory.domain.WarehouseCode;
import io.d4tzz.ecommerce.inventory.domain.WarehouseId;
import io.d4tzz.ecommerce.inventory.domain.WarehouseRepository;
import io.d4tzz.ecommerce.inventory.domain.WarehouseStatus;
import io.d4tzz.ecommerce.inventory.query.StockQueryService;
import io.d4tzz.ecommerce.inventory.query.VariantAvailabilityView;
import io.d4tzz.ecommerce.inventory.query.WarehouseQueryService;
import io.d4tzz.ecommerce.inventory.query.WarehouseView;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class InventoryUseCasesTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Address ADDRESS = Address.of("Ha Noi", "Cau Giay", "Dich Vong", "1 Xuan Thuy");
    private static final long WAREHOUSE = 1L;
    private static final long ORDER = 9L;
    private static final long A = 10L;
    private static final long B = 11L;

    private WarehouseRepository warehouseRepository;
    private InventoryItemRepository itemRepository;
    private StockReservationRepository reservationRepository;
    private StockLedgerRepository ledgerRepository;
    private VariantExistenceChecker variantChecker;
    private StockQueryService stockQueryService;
    private WarehouseQueryService warehouseQueryService;
    private final StockReservationService reservationService = new StockReservationService();

    private CreateWarehouseUseCase createWarehouse;
    private UpdateWarehouseUseCase updateWarehouse;
    private ChangeWarehouseStatusUseCase changeStatus;
    private ReceiveGoodsUseCase receiveGoods;
    private ReserveStockUseCase reserveStock;
    private ReleaseStockUseCase releaseStock;
    private CommitStockUseCase commitStock;
    private RestockReturnedUseCase restockReturned;
    private AllocateStockUseCase allocateStock;

    @BeforeEach
    void setUp() {
        warehouseRepository = mock(WarehouseRepository.class);
        itemRepository = mock(InventoryItemRepository.class);
        reservationRepository = mock(StockReservationRepository.class);
        ledgerRepository = mock(StockLedgerRepository.class);
        variantChecker = mock(VariantExistenceChecker.class);
        stockQueryService = mock(StockQueryService.class);
        warehouseQueryService = mock(WarehouseQueryService.class);

        when(variantChecker.findMissing(any())).thenReturn(Set.of());
        when(itemRepository.findAllForUpdate(any(), any())).thenReturn(Map.of());

        createWarehouse = new CreateWarehouseUseCase(warehouseRepository);
        updateWarehouse = new UpdateWarehouseUseCase(warehouseRepository);
        changeStatus = new ChangeWarehouseStatusUseCase(warehouseRepository);
        receiveGoods = new ReceiveGoodsUseCase(warehouseRepository, itemRepository, ledgerRepository, variantChecker);
        reserveStock = new ReserveStockUseCase(
            warehouseRepository,
            itemRepository,
            reservationRepository,
            ledgerRepository,
            reservationService
        );
        releaseStock = new ReleaseStockUseCase(itemRepository, reservationRepository, ledgerRepository, reservationService);
        commitStock = new CommitStockUseCase(itemRepository, reservationRepository, ledgerRepository, reservationService);
        restockReturned = new RestockReturnedUseCase(
            itemRepository,
            reservationRepository,
            ledgerRepository,
            reservationService
        );
        allocateStock = new AllocateStockUseCase(stockQueryService, warehouseQueryService, new StockAllocator());
    }

    // ---- fixtures ----

    private static Warehouse warehouse(long id, WarehouseStatus status) {
        return Warehouse.restore(WarehouseId.of(id), WarehouseCode.of("HN-0" + id), "Hanoi", ADDRESS, status, T0, T0);
    }

    private Warehouse existingWarehouse(WarehouseStatus status) {
        Warehouse warehouse = warehouse(WAREHOUSE, status);
        when(warehouseRepository.find(WarehouseId.of(WAREHOUSE))).thenReturn(Optional.of(warehouse));
        return warehouse;
    }

    private static InventoryItem item(long variantId, int onHand, int reserved) {
        return InventoryItem.restore(
            InventoryItemId.of(variantId),
            WarehouseId.of(WAREHOUSE),
            VariantId.of(variantId),
            onHand,
            reserved,
            T0,
            T0
        );
    }

    private void itemsExist(InventoryItem... items) {
        Map<VariantId, InventoryItem> map = new HashMap<>();
        for (InventoryItem item : items) {
            map.put(item.getVariantId(), item);
        }
        when(itemRepository.findAllForUpdate(eq(WarehouseId.of(WAREHOUSE)), any())).thenReturn(map);
    }

    private static StockReservation reservation(ReservationStatus status, long... variantIds) {
        List<ReservationLine> lines = java.util.Arrays
            .stream(variantIds)
            .mapToObj(id -> new ReservationLine(VariantId.of(id), 2))
            .toList();
        return StockReservation.restore(
            StockReservationId.of(1L),
            ORDER,
            WarehouseId.of(WAREHOUSE),
            lines,
            status,
            T0,
            T0
        );
    }

    private void orderHolds(StockReservation... reservations) {
        when(reservationRepository.findAllForUpdate(ORDER)).thenReturn(List.of(reservations));
    }

    @SuppressWarnings("unchecked")
    private void assertSavedItems(InventoryItem... expected) {
        ArgumentCaptor<java.util.Collection<InventoryItem>> captor = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(itemRepository).saveAll(captor.capture());
        assertEquals(List.of(expected), List.copyOf(captor.getValue()));
    }

    @SuppressWarnings("unchecked")
    private List<StockLedgerEntry> savedEntries() {
        ArgumentCaptor<List<StockLedgerEntry>> captor = ArgumentCaptor.forClass(List.class);
        verify(ledgerRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    // ---- create warehouse ----

    @Test
    void createWarehouse_savesAnActiveWarehouseAndReturnsItsId() {
        when(warehouseRepository.save(any())).thenReturn(warehouse(5L, WarehouseStatus.ACTIVE));

        Long id = createWarehouse.execute(
            new CreateWarehouseCommand("hn-01", "Hanoi", "Ha Noi", "Cau Giay", "Dich Vong", "1 Xuan Thuy")
        );

        assertEquals(5L, id);
        ArgumentCaptor<Warehouse> captor = ArgumentCaptor.forClass(Warehouse.class);
        verify(warehouseRepository).save(captor.capture());
        assertEquals(WarehouseCode.of("HN-01"), captor.getValue().getCode());
        assertTrue(captor.getValue().isActive());
        assertEquals(ADDRESS, captor.getValue().getAddress());
    }

    @Test
    void createWarehouse_failsWhenTheCodeIsTaken() {
        when(warehouseRepository.existsByCode(WarehouseCode.of("HN-01"))).thenReturn(true);

        assertThrows(
            ConflictException.class,
            () -> createWarehouse.execute(
                new CreateWarehouseCommand("HN-01", "Hanoi", "Ha Noi", "Cau Giay", "Dich Vong", "1 Xuan Thuy")
            )
        );
        verify(warehouseRepository, never()).save(any());
    }

    @Test
    void createWarehouse_rejectsAnInvalidCodeAndSavesNothing() {
        assertThrows(
            InvalidArgumentException.class,
            () -> createWarehouse.execute(
                new CreateWarehouseCommand("a b", "Hanoi", "Ha Noi", "Cau Giay", "Dich Vong", "1 Xuan Thuy")
            )
        );
        verify(warehouseRepository, never()).save(any());
    }

    // ---- update warehouse ----

    @Test
    void updateWarehouse_changesNameAndAddress() {
        Warehouse warehouse = existingWarehouse(WarehouseStatus.ACTIVE);

        updateWarehouse.execute(new UpdateWarehouseCommand(WAREHOUSE, "Danang", "Da Nang", "Hai Chau", "Thach Thang", "2 Bach Dang"));

        assertEquals("Danang", warehouse.getName());
        assertEquals("Da Nang", warehouse.getAddress().province());
        verify(warehouseRepository).save(warehouse);
    }

    @Test
    void updateWarehouse_failsWhenTheWarehouseDoesNotExist() {
        when(warehouseRepository.find(any())).thenReturn(Optional.empty());

        assertThrows(
            NotFoundException.class,
            () -> updateWarehouse.execute(new UpdateWarehouseCommand(99L, "x", "p", "d", "w", "a"))
        );
        verify(warehouseRepository, never()).save(any());
    }

    // ---- change status ----

    @Test
    void changeStatus_deactivatesAnActiveWarehouse() {
        Warehouse warehouse = existingWarehouse(WarehouseStatus.ACTIVE);

        changeStatus.execute(new ChangeWarehouseStatusCommand(WAREHOUSE, WarehouseStatus.INACTIVE));

        assertFalse(warehouse.isActive());
        verify(warehouseRepository).save(warehouse);
    }

    @Test
    void changeStatus_activatesAnInactiveWarehouse() {
        Warehouse warehouse = existingWarehouse(WarehouseStatus.INACTIVE);

        changeStatus.execute(new ChangeWarehouseStatusCommand(WAREHOUSE, WarehouseStatus.ACTIVE));

        assertTrue(warehouse.isActive());
        verify(warehouseRepository).save(warehouse);
    }

    @Test
    void changeStatus_savesNothingWhenTheStatusIsAlreadySet() {
        existingWarehouse(WarehouseStatus.ACTIVE);

        changeStatus.execute(new ChangeWarehouseStatusCommand(WAREHOUSE, WarehouseStatus.ACTIVE));

        verify(warehouseRepository, never()).save(any());
    }

    @Test
    void changeStatus_failsWhenTheWarehouseDoesNotExist() {
        when(warehouseRepository.find(any())).thenReturn(Optional.empty());

        assertThrows(
            NotFoundException.class,
            () -> changeStatus.execute(new ChangeWarehouseStatusCommand(99L, WarehouseStatus.INACTIVE))
        );
    }

    // ---- receive goods ----

    private ReceiveGoodsCommand receipt(String code, ReceiveGoodsCommand.Item... items) {
        return new ReceiveGoodsCommand(WAREHOUSE, code, "note", List.of(items));
    }

    @Test
    void receiveGoods_createsAnItemForANewVariantAndRecordsTheLedger() {
        existingWarehouse(WarehouseStatus.ACTIVE);

        List<UUID> ids = receiveGoods.execute(receipt("GR-1", new ReceiveGoodsCommand.Item(A, 5)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Collection<InventoryItem>> items = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(itemRepository).saveAll(items.capture());
        InventoryItem saved = items.getValue().iterator().next();
        assertNull(saved.getId());
        assertEquals(5, saved.getOnHand());

        List<StockLedgerEntry> entries = savedEntries();
        assertEquals(1, entries.size());
        assertEquals(StockLedgerEntryType.RECEIVE, entries.get(0).type());
        assertEquals(StockLedgerReference.goodsReceipt("GR-1"), entries.get(0).reference());
        assertEquals("note", entries.get(0).note());
        assertEquals(List.of(entries.get(0).id().value()), ids);
    }

    @Test
    void receiveGoods_addsToAnExistingItem() {
        existingWarehouse(WarehouseStatus.ACTIVE);
        InventoryItem existing = item(A, 3, 1);
        itemsExist(existing);

        receiveGoods.execute(receipt("GR-1", new ReceiveGoodsCommand.Item(A, 5)));

        assertEquals(8, existing.getOnHand());
        assertEquals(1, existing.getReserved());
    }

    @Test
    void receiveGoods_sumsAVariantListedTwiceIntoOneEntry() {
        existingWarehouse(WarehouseStatus.ACTIVE);

        receiveGoods.execute(receipt("GR-1", new ReceiveGoodsCommand.Item(A, 5), new ReceiveGoodsCommand.Item(A, 2)));

        List<StockLedgerEntry> entries = savedEntries();
        assertEquals(1, entries.size());
        assertEquals(7, entries.get(0).quantity());
    }

    @Test
    void receiveGoods_failsWhenTheWarehouseDoesNotExist() {
        when(warehouseRepository.find(any())).thenReturn(Optional.empty());

        assertThrows(
            NotFoundException.class,
            () -> receiveGoods.execute(receipt("GR-1", new ReceiveGoodsCommand.Item(A, 5)))
        );
        verifyNoInteractions(itemRepository, ledgerRepository);
    }

    @Test
    void receiveGoods_failsWhenTheWarehouseIsInactive() {
        existingWarehouse(WarehouseStatus.INACTIVE);

        assertThrows(
            InvalidOperationException.class,
            () -> receiveGoods.execute(receipt("GR-1", new ReceiveGoodsCommand.Item(A, 5)))
        );
        verifyNoInteractions(itemRepository, ledgerRepository);
    }

    @Test
    void receiveGoods_failsWhenAVariantDoesNotExistInTheCatalog() {
        existingWarehouse(WarehouseStatus.ACTIVE);
        when(variantChecker.findMissing(any())).thenReturn(Set.of(VariantId.of(B)));

        var thrown = assertThrows(
            NotFoundException.class,
            () -> receiveGoods.execute(receipt("GR-1", new ReceiveGoodsCommand.Item(A, 5), new ReceiveGoodsCommand.Item(B, 1)))
        );

        assertTrue(thrown.getMessage().contains(String.valueOf(B)));
        verify(itemRepository, never()).saveAll(any());
        verify(ledgerRepository, never()).saveAll(any());
    }

    @Test
    void receiveGoods_rejectsTheSameReceiptTwice() {
        existingWarehouse(WarehouseStatus.ACTIVE);
        when(ledgerRepository.existsByReference(WarehouseId.of(WAREHOUSE), StockLedgerReference.goodsReceipt("GR-1")))
            .thenReturn(true);

        assertThrows(
            ConflictException.class,
            () -> receiveGoods.execute(receipt("GR-1", new ReceiveGoodsCommand.Item(A, 5)))
        );
        verify(itemRepository, never()).saveAll(any());
        verify(ledgerRepository, never()).saveAll(any());
    }

    @Test
    void receiveGoods_locksTheItemsBeforeLookingForTheReceipt() {
        existingWarehouse(WarehouseStatus.ACTIVE);

        receiveGoods.execute(receipt("GR-1", new ReceiveGoodsCommand.Item(A, 5)));

        var inOrder = inOrder(itemRepository, ledgerRepository);
        inOrder.verify(itemRepository).findAllForUpdate(any(), any());
        inOrder.verify(ledgerRepository).existsByReference(any(), any());
    }

    // ---- reserve ----

    private ReserveStockCommand reserveCommand(ReserveStockCommand.Line... lines) {
        return new ReserveStockCommand(ORDER, WAREHOUSE, List.of(lines));
    }

    @Test
    void reserveStock_holdsTheStockAndSavesReservationItemsAndLedger() {
        existingWarehouse(WarehouseStatus.ACTIVE);
        InventoryItem a = item(A, 5, 0);
        itemsExist(a);

        reserveStock.execute(reserveCommand(new ReserveStockCommand.Line(A, 2)));

        assertEquals(2, a.getReserved());
        ArgumentCaptor<StockReservation> reservation = ArgumentCaptor.forClass(StockReservation.class);
        verify(reservationRepository).save(reservation.capture());
        assertEquals(ReservationStatus.RESERVED, reservation.getValue().getStatus());
        assertEquals(ORDER, reservation.getValue().getOrderId());
        assertSavedItems(a);
        assertEquals(StockLedgerEntryType.RESERVE, savedEntries().get(0).type());
    }

    @Test
    void reserveStock_doesNothingWhenTheOrderAlreadyHoldsStockInTheWarehouse() {
        when(reservationRepository.exists(ORDER, WarehouseId.of(WAREHOUSE))).thenReturn(true);

        reserveStock.execute(reserveCommand(new ReserveStockCommand.Line(A, 2)));

        verifyNoInteractions(warehouseRepository, itemRepository, ledgerRepository);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void reserveStock_failsWhenTheWarehouseDoesNotExist() {
        when(warehouseRepository.find(any())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> reserveStock.execute(reserveCommand(new ReserveStockCommand.Line(A, 2))));
    }

    @Test
    void reserveStock_failsWhenTheWarehouseIsInactive() {
        existingWarehouse(WarehouseStatus.INACTIVE);
        itemsExist(item(A, 5, 0));

        assertThrows(
            InvalidOperationException.class,
            () -> reserveStock.execute(reserveCommand(new ReserveStockCommand.Line(A, 2)))
        );
        verify(reservationRepository, never()).save(any());
        verify(ledgerRepository, never()).saveAll(any());
    }

    @Test
    void reserveStock_failsAndSavesNothingWhenOneLineIsOutOfStock() {
        existingWarehouse(WarehouseStatus.ACTIVE);
        itemsExist(item(A, 5, 0), item(B, 1, 1));

        assertThrows(
            OutOfStockException.class,
            () -> reserveStock.execute(
                reserveCommand(new ReserveStockCommand.Line(A, 2), new ReserveStockCommand.Line(B, 1))
            )
        );
        verify(itemRepository, never()).saveAll(any());
        verify(reservationRepository, never()).save(any());
        verify(ledgerRepository, never()).saveAll(any());
    }

    @Test
    void reserveStock_failsWhenTheWarehouseNeverStockedTheVariant() {
        existingWarehouse(WarehouseStatus.ACTIVE);

        assertThrows(
            OutOfStockException.class,
            () -> reserveStock.execute(reserveCommand(new ReserveStockCommand.Line(A, 2)))
        );
    }

    // ---- release / commit / restock ----

    @Test
    void releaseStock_releasesEveryReservationOfTheOrder() {
        InventoryItem a = item(A, 5, 2);
        itemsExist(a);
        StockReservation reservation = reservation(ReservationStatus.RESERVED, A);
        orderHolds(reservation);

        releaseStock.execute(new ReleaseStockCommand(ORDER));

        assertEquals(ReservationStatus.RELEASED, reservation.getStatus());
        assertEquals(0, a.getReserved());
        assertEquals(5, a.getOnHand());
        verify(reservationRepository).save(reservation);
        assertSavedItems(a);
        assertEquals(StockLedgerEntryType.RELEASE, savedEntries().get(0).type());
    }

    @Test
    void releaseStock_skipsReservationsAlreadyReleased() {
        itemsExist(item(A, 5, 0));
        orderHolds(reservation(ReservationStatus.RELEASED, A));

        releaseStock.execute(new ReleaseStockCommand(ORDER));

        verify(reservationRepository, never()).save(any());
        verify(itemRepository, never()).saveAll(any());
        verify(ledgerRepository, never()).saveAll(any());
    }

    @Test
    void releaseStock_doesNothingForAnOrderWithoutReservations() {
        orderHolds();

        releaseStock.execute(new ReleaseStockCommand(ORDER));

        verifyNoInteractions(itemRepository, ledgerRepository);
    }

    @Test
    void releaseStock_rejectsAnOrderAlreadyShipped() {
        itemsExist(item(A, 5, 0));
        orderHolds(reservation(ReservationStatus.COMMITTED, A));

        assertThrows(InvalidOperationException.class, () -> releaseStock.execute(new ReleaseStockCommand(ORDER)));
        verify(ledgerRepository, never()).saveAll(any());
    }

    @Test
    void releaseStock_worksOnAnInactiveWarehouse() {
        // The use case never loads the warehouse, so deactivating one does not strand its reservations
        InventoryItem a = item(A, 5, 2);
        itemsExist(a);
        orderHolds(reservation(ReservationStatus.RESERVED, A));

        releaseStock.execute(new ReleaseStockCommand(ORDER));

        verifyNoInteractions(warehouseRepository);
        assertEquals(0, a.getReserved());
    }

    @Test
    void commitStock_takesTheUnitsOutOfEveryWarehouseOfTheOrder() {
        InventoryItem a = item(A, 5, 2);
        itemsExist(a);
        StockReservation reservation = reservation(ReservationStatus.RESERVED, A);
        orderHolds(reservation);

        commitStock.execute(new CommitStockCommand(ORDER));

        assertEquals(ReservationStatus.COMMITTED, reservation.getStatus());
        assertEquals(3, a.getOnHand());
        assertEquals(0, a.getReserved());
        assertEquals(StockLedgerEntryType.COMMIT, savedEntries().get(0).type());
    }

    @Test
    void commitStock_skipsReservationsAlreadyCommitted() {
        itemsExist(item(A, 5, 0));
        orderHolds(reservation(ReservationStatus.COMMITTED, A));

        commitStock.execute(new CommitStockCommand(ORDER));

        verify(ledgerRepository, never()).saveAll(any());
    }

    @Test
    void commitStock_rejectsAReleasedReservation() {
        itemsExist(item(A, 5, 0));
        orderHolds(reservation(ReservationStatus.RELEASED, A));

        assertThrows(InvalidOperationException.class, () -> commitStock.execute(new CommitStockCommand(ORDER)));
    }

    @Test
    void restockReturned_putsTheUnitsBackOnHand() {
        InventoryItem a = item(A, 3, 0);
        itemsExist(a);
        StockReservation reservation = reservation(ReservationStatus.COMMITTED, A);
        orderHolds(reservation);

        restockReturned.execute(new RestockReturnedCommand(ORDER));

        assertEquals(ReservationStatus.RESTOCKED, reservation.getStatus());
        assertEquals(5, a.getOnHand());
        assertEquals(StockLedgerEntryType.RESTOCK, savedEntries().get(0).type());
    }

    @Test
    void restockReturned_skipsReservationsAlreadyRestocked() {
        itemsExist(item(A, 5, 0));
        orderHolds(reservation(ReservationStatus.RESTOCKED, A));

        restockReturned.execute(new RestockReturnedCommand(ORDER));

        verify(ledgerRepository, never()).saveAll(any());
    }

    @Test
    void restockReturned_rejectsAReservationThatNeverLeft() {
        itemsExist(item(A, 5, 2));
        orderHolds(reservation(ReservationStatus.RESERVED, A));

        assertThrows(InvalidOperationException.class, () -> restockReturned.execute(new RestockReturnedCommand(ORDER)));
    }

    @Test
    void commitStock_handlesEveryReservationOfTheOrder() {
        // Both reservations use the same item here, which holds enough for both
        InventoryItem a = item(A, 5, 4);
        itemsExist(a);
        StockReservation first = reservation(ReservationStatus.RESERVED, A);
        StockReservation second = reservation(ReservationStatus.RESERVED, A);
        orderHolds(first, second);

        commitStock.execute(new CommitStockCommand(ORDER));

        assertEquals(ReservationStatus.COMMITTED, first.getStatus());
        assertEquals(ReservationStatus.COMMITTED, second.getStatus());
        verify(reservationRepository, times(2)).save(any());
        assertEquals(1, a.getOnHand());
        assertEquals(0, a.getReserved());
    }

    // ---- allocate ----

    private void stockIs(VariantAvailabilityView... views) {
        when(stockQueryService.getAvailability(any())).thenReturn(List.of(views));
    }

    private void warehousesAre(WarehouseView... views) {
        when(warehouseQueryService.getWarehouses()).thenReturn(List.of(views));
    }

    private static WarehouseView warehouseView(long id, String province) {
        return new WarehouseView(id, "W" + id, "Warehouse " + id, province, "d", "w", "a", "ACTIVE", T0, T0);
    }

    private static VariantAvailabilityView availability(long variant, long warehouse, int available) {
        return new VariantAvailabilityView(variant, warehouse, "W" + warehouse, available);
    }

    @Test
    void allocateStock_returnsOneEntryPerWarehouse() {
        stockIs(availability(A, 1, 5), availability(B, 2, 5));
        warehousesAre(warehouseView(1, "Ha Noi"), warehouseView(2, "Da Nang"));

        List<StockAllocation> result = allocateStock.execute(
            new AllocateStockCommand("Ha Noi", List.of(new AllocateStockCommand.Line(A, 2), new AllocateStockCommand.Line(B, 1)))
        );

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).warehouseId());
        assertEquals(List.of(new StockAllocation.Line(A, 2)), result.get(0).lines());
        assertEquals(2L, result.get(1).warehouseId());
        assertEquals(List.of(new StockAllocation.Line(B, 1)), result.get(1).lines());
    }

    @Test
    void allocateStock_prefersAWarehouseInTheDestinationProvince() {
        stockIs(availability(A, 1, 5), availability(A, 2, 5));
        warehousesAre(warehouseView(1, "Ha Noi"), warehouseView(2, "Da Nang"));

        List<StockAllocation> result = allocateStock.execute(
            new AllocateStockCommand("  da nang ", List.of(new AllocateStockCommand.Line(A, 1)))
        );

        assertEquals(List.of(2L), result.stream().map(StockAllocation::warehouseId).toList());
    }

    @Test
    void allocateStock_ignoresWarehousesWithNoAvailableUnits() {
        stockIs(availability(A, 1, 0), availability(A, 2, 3));
        warehousesAre(warehouseView(1, "Ha Noi"), warehouseView(2, "Da Nang"));

        List<StockAllocation> result = allocateStock.execute(
            new AllocateStockCommand("Ha Noi", List.of(new AllocateStockCommand.Line(A, 1)))
        );

        assertEquals(List.of(2L), result.stream().map(StockAllocation::warehouseId).toList());
    }

    @Test
    void allocateStock_failsWhenTheStockIsNotEnough() {
        stockIs(availability(A, 1, 1));
        warehousesAre(warehouseView(1, "Ha Noi"));

        assertThrows(
            OutOfStockException.class,
            () -> allocateStock.execute(new AllocateStockCommand("Ha Noi", List.of(new AllocateStockCommand.Line(A, 2))))
        );
    }

    @Test
    void allocateStock_asksForTheAvailabilityOfEachVariantOnce() {
        stockIs(availability(A, 1, 9));
        warehousesAre(warehouseView(1, "Ha Noi"));

        allocateStock.execute(
            new AllocateStockCommand("Ha Noi", List.of(new AllocateStockCommand.Line(A, 1), new AllocateStockCommand.Line(A, 2)))
        );

        verify(stockQueryService).getAvailability(List.of(A));
    }
}
