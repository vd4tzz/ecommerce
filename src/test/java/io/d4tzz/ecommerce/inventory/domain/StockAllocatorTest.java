package io.d4tzz.ecommerce.inventory.domain;

import static org.junit.jupiter.api.Assertions.*;

import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StockAllocatorTest {

    private static final VariantId A = VariantId.of(1L);
    private static final VariantId B = VariantId.of(2L);

    private final StockAllocator allocator = new StockAllocator();

    private static WarehouseStock warehouse(long id, boolean near, Map<VariantId, Integer> available) {
        return new WarehouseStock(WarehouseId.of(id), near, available);
    }

    private static ReservationLine line(VariantId variant, int quantity) {
        return new ReservationLine(variant, quantity);
    }

    private static List<Long> warehouseIds(List<WarehouseAllocation> allocations) {
        return allocations.stream().map(a -> a.warehouseId().value()).toList();
    }

    // ---- one warehouse ----

    @Test
    void aWarehouseThatHasEverythingSendsTheWholeOrder() {
        var result = allocator.allocate(
            List.of(line(A, 2), line(B, 1)),
            List.of(warehouse(1, false, Map.of(A, 5, B, 5)), warehouse(2, false, Map.of(A, 1)))
        );

        assertEquals(1, result.size());
        assertEquals(WarehouseId.of(1L), result.get(0).warehouseId());
        assertEquals(List.of(line(A, 2), line(B, 1)), result.get(0).lines());
    }

    @Test
    void aWarehouseWithEverythingBeatsABiggerButIncompleteOne() {
        var result = allocator.allocate(
            List.of(line(A, 2), line(B, 2)),
            List.of(warehouse(1, false, Map.of(A, 100)), warehouse(2, false, Map.of(A, 2, B, 2)))
        );

        assertEquals(List.of(2L), warehouseIds(result));
    }

    @Test
    void onlyTakesWhatTheOrderNeeds() {
        var result = allocator.allocate(List.of(line(A, 2)), List.of(warehouse(1, false, Map.of(A, 50))));

        assertEquals(List.of(line(A, 2)), result.get(0).lines());
    }

    @Test
    void duplicateLinesOfAVariantAreSummed() {
        var result = allocator.allocate(
            List.of(line(A, 2), line(A, 3)),
            List.of(warehouse(1, false, Map.of(A, 5)))
        );

        assertEquals(List.of(line(A, 5)), result.get(0).lines());
    }

    // ---- ties ----

    @Test
    void onATiePrefersAWarehouseNearTheDestination() {
        var result = allocator.allocate(
            List.of(line(A, 1)),
            List.of(warehouse(1, false, Map.of(A, 5)), warehouse(2, true, Map.of(A, 5)))
        );

        assertEquals(List.of(2L), warehouseIds(result));
    }

    @Test
    void onALaterTiePrefersTheLowestId() {
        var result = allocator.allocate(
            List.of(line(A, 1)),
            List.of(warehouse(3, false, Map.of(A, 5)), warehouse(2, false, Map.of(A, 5)))
        );

        assertEquals(List.of(2L), warehouseIds(result));
    }

    @Test
    void aFarWarehouseThatCoversMoreBeatsANearOneThatCoversLess() {
        var result = allocator.allocate(
            List.of(line(A, 4)),
            List.of(warehouse(1, true, Map.of(A, 1)), warehouse(2, false, Map.of(A, 4)))
        );

        assertEquals(List.of(2L), warehouseIds(result));
    }

    // ---- several warehouses ----

    @Test
    void splitsTheOrderWhenNoWarehouseHasEverything() {
        var result = allocator.allocate(
            List.of(line(A, 3), line(B, 2)),
            List.of(warehouse(1, false, Map.of(A, 3)), warehouse(2, false, Map.of(B, 2)))
        );

        assertEquals(List.of(1L, 2L), warehouseIds(result));
        assertEquals(List.of(line(A, 3)), result.get(0).lines());
        assertEquals(List.of(line(B, 2)), result.get(1).lines());
    }

    @Test
    void takesTheRestOfAVariantFromTheNextWarehouse() {
        var result = allocator.allocate(
            List.of(line(A, 5)),
            List.of(warehouse(1, false, Map.of(A, 3)), warehouse(2, false, Map.of(A, 4)))
        );

        assertEquals(List.of(1L, 2L), warehouseIds(result));
        assertEquals(5, result.stream().flatMap(a -> a.lines().stream()).mapToInt(ReservationLine::quantity).sum());
        // The bigger one goes first and gives 4; the other gives the last unit
        assertEquals(List.of(line(A, 1)), result.get(0).lines());
        assertEquals(List.of(line(A, 4)), result.get(1).lines());
    }

    @Test
    void returnsAllocationsInAscendingWarehouseIdOrder() {
        var result = allocator.allocate(
            List.of(line(A, 5)),
            List.of(warehouse(9, false, Map.of(A, 4)), warehouse(2, false, Map.of(A, 1)))
        );

        assertEquals(List.of(2L, 9L), warehouseIds(result));
    }

    @Test
    void doesNotUseAWarehouseThatAddsNothing() {
        var result = allocator.allocate(
            List.of(line(A, 2)),
            List.of(warehouse(1, false, Map.of(A, 2)), warehouse(2, false, Map.of(B, 9)))
        );

        assertEquals(List.of(1L), warehouseIds(result));
    }

    // ---- failures ----

    @Test
    void failsWhenAllWarehousesTogetherHaveTooLittle() {
        var thrown = assertThrows(
            OutOfStockException.class,
            () -> allocator.allocate(
                List.of(line(A, 5)),
                List.of(warehouse(1, false, Map.of(A, 2)), warehouse(2, false, Map.of(A, 2)))
            )
        );

        assertTrue(thrown.getMessage().contains("out of stock"));
    }

    @Test
    void failsWhenThereAreNoWarehouses() {
        assertThrows(OutOfStockException.class, () -> allocator.allocate(List.of(line(A, 1)), List.of()));
    }

    @Test
    void failsWhenThereIsNothingToAllocate() {
        assertThrows(
            InvalidArgumentException.class,
            () -> allocator.allocate(List.of(), List.of(warehouse(1, false, Map.of(A, 1))))
        );
    }
}
