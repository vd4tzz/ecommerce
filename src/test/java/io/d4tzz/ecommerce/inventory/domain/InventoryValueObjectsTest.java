package io.d4tzz.ecommerce.inventory.domain;

import static org.junit.jupiter.api.Assertions.*;

import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InventoryValueObjectsTest {

    // ---- WarehouseCode ----

    @Test
    void warehouseCode_isTrimmedAndUpperCased() {
        assertEquals("HN-01", WarehouseCode.of("  hn-01 ").value());
        assertEquals("HN-01", WarehouseCode.of("hn-01").toString());
    }

    @Test
    void warehouseCode_equalsIgnoringCase() {
        assertEquals(WarehouseCode.of("hn_01"), WarehouseCode.of("HN_01"));
    }

    @Test
    void warehouseCode_acceptsTwoToFiftyCharacters() {
        assertDoesNotThrow(() -> WarehouseCode.of("AB"));
        assertDoesNotThrow(() -> WarehouseCode.of("A".repeat(50)));
    }

    @Test
    void warehouseCode_rejectsTooShortTooLongAndOtherCharacters() {
        assertThrows(InvalidArgumentException.class, () -> WarehouseCode.of("A"));
        assertThrows(InvalidArgumentException.class, () -> WarehouseCode.of("A".repeat(51)));
        assertThrows(InvalidArgumentException.class, () -> WarehouseCode.of("HN 01"));
        assertThrows(InvalidArgumentException.class, () -> WarehouseCode.of("HN.01"));
        assertThrows(InvalidArgumentException.class, () -> WarehouseCode.of("   "));
    }

    // ---- StockLedgerReference ----

    @Test
    void ledgerReference_forAnOrderUsesTheOrderId() {
        StockLedgerReference reference = StockLedgerReference.order(42L);

        assertEquals(StockLedgerReference.Type.ORDER, reference.type());
        assertEquals("42", reference.value());
    }

    @Test
    void ledgerReference_forAGoodsReceiptTrimsTheCode() {
        StockLedgerReference reference = StockLedgerReference.goodsReceipt("  GR-7 ");

        assertEquals(StockLedgerReference.Type.GOODS_RECEIPT, reference.type());
        assertEquals("GR-7", reference.value());
    }

    @Test
    void ledgerReference_rejectsABlankValue() {
        assertThrows(InvalidArgumentException.class, () -> StockLedgerReference.goodsReceipt("   "));
    }

    // ---- ReservationLine ----

    @Test
    void reservationLine_requiresAPositiveQuantity() {
        assertDoesNotThrow(() -> new ReservationLine(VariantId.of(1L), 1));
        assertThrows(InvalidArgumentException.class, () -> new ReservationLine(VariantId.of(1L), 0));
        assertThrows(InvalidArgumentException.class, () -> new ReservationLine(VariantId.of(1L), -2));
    }

    // ---- ReservationStatus ----

    @Test
    void reservationStatus_allowsOnlyTheLifecycleMoves() {
        assertTrue(ReservationStatus.RESERVED.canTransitionTo(ReservationStatus.RELEASED));
        assertTrue(ReservationStatus.RESERVED.canTransitionTo(ReservationStatus.COMMITTED));
        assertTrue(ReservationStatus.COMMITTED.canTransitionTo(ReservationStatus.RESTOCKED));

        assertFalse(ReservationStatus.RESERVED.canTransitionTo(ReservationStatus.RESTOCKED));
        assertFalse(ReservationStatus.COMMITTED.canTransitionTo(ReservationStatus.RELEASED));
        assertFalse(ReservationStatus.COMMITTED.canTransitionTo(ReservationStatus.RESERVED));
    }

    @Test
    void reservationStatus_releasedAndRestockedAreFinal() {
        for (ReservationStatus next : ReservationStatus.values()) {
            assertFalse(ReservationStatus.RELEASED.canTransitionTo(next));
            assertFalse(ReservationStatus.RESTOCKED.canTransitionTo(next));
        }
    }

    // ---- WarehouseStock ----

    @Test
    void warehouseStock_availableOfIsZeroForAnUnknownVariant() {
        var stock = new WarehouseStock(WarehouseId.of(1L), false, Map.of(VariantId.of(1L), 4));

        assertEquals(4, stock.availableOf(VariantId.of(1L)));
        assertEquals(0, stock.availableOf(VariantId.of(2L)));
    }

    @Test
    void warehouseStock_availableOfNeverGoesBelowZero() {
        var stock = new WarehouseStock(WarehouseId.of(1L), false, Map.of(VariantId.of(1L), -3));

        assertEquals(0, stock.availableOf(VariantId.of(1L)));
    }

    // ---- WarehouseAllocation ----

    @Test
    void warehouseAllocation_copiesItsLines() {
        List<ReservationLine> lines = new ArrayList<>(List.of(new ReservationLine(VariantId.of(1L), 1)));
        var allocation = new WarehouseAllocation(WarehouseId.of(1L), lines);

        lines.clear();

        assertEquals(1, allocation.lines().size());
    }
}
