package io.d4tzz.ecommerce.inventory.domain;

import static org.junit.jupiter.api.Assertions.*;

import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class StockReservationTest {

    private static final WarehouseId WAREHOUSE = WarehouseId.of(1L);
    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = T0.plusSeconds(60);

    private static ReservationLine line(long variantId, int quantity) {
        return new ReservationLine(VariantId.of(variantId), quantity);
    }

    private static StockReservation reservation() {
        return StockReservation.create(9L, WAREHOUSE, List.of(line(1, 2)), T0);
    }

    private static StockReservation reservationIn(ReservationStatus status) {
        return StockReservation.restore(StockReservationId.of(1L), 9L, WAREHOUSE, List.of(line(1, 2)), status, T0, T0);
    }

    // ---- creating ----

    @Test
    void create_startsReservedWithoutAnId() {
        StockReservation reservation = reservation();

        assertNull(reservation.getId());
        assertEquals(9L, reservation.getOrderId());
        assertEquals(WAREHOUSE, reservation.getWarehouseId());
        assertEquals(ReservationStatus.RESERVED, reservation.getStatus());
        assertEquals(T0, reservation.getCreatedAt());
        assertEquals(T0, reservation.getUpdatedAt());
    }

    @Test
    void create_sumsDuplicateVariantsAndSortsByVariantId() {
        StockReservation reservation = StockReservation.create(
            9L,
            WAREHOUSE,
            List.of(line(3, 1), line(1, 2), line(3, 4)),
            T0
        );

        assertEquals(List.of(line(1, 2), line(3, 5)), reservation.getLines());
        assertEquals(List.of(VariantId.of(1L), VariantId.of(3L)), reservation.variantIds());
    }

    @Test
    void create_requiresAtLeastOneLine() {
        assertThrows(InvalidArgumentException.class, () -> StockReservation.create(9L, WAREHOUSE, List.of(), T0));
    }

    @Test
    void getLines_cannotBeModified() {
        assertThrows(UnsupportedOperationException.class, () -> reservation().getLines().clear());
    }

    // ---- status changes ----

    @Test
    void release_movesFromReservedToReleased() {
        StockReservation reservation = reservation();

        assertTrue(reservation.release(T1));

        assertEquals(ReservationStatus.RELEASED, reservation.getStatus());
        assertEquals(T1, reservation.getUpdatedAt());
    }

    @Test
    void commit_movesFromReservedToCommitted() {
        StockReservation reservation = reservation();

        assertTrue(reservation.commit(T1));

        assertEquals(ReservationStatus.COMMITTED, reservation.getStatus());
    }

    @Test
    void restock_movesFromCommittedToRestocked() {
        StockReservation reservation = reservationIn(ReservationStatus.COMMITTED);

        assertTrue(reservation.restock(T1));

        assertEquals(ReservationStatus.RESTOCKED, reservation.getStatus());
    }

    @Test
    void repeatingTheCurrentStatusChangesNothingAndReturnsFalse() {
        StockReservation released = reservationIn(ReservationStatus.RELEASED);
        StockReservation committed = reservationIn(ReservationStatus.COMMITTED);
        StockReservation restocked = reservationIn(ReservationStatus.RESTOCKED);

        assertFalse(released.release(T1));
        assertFalse(committed.commit(T1));
        assertFalse(restocked.restock(T1));

        assertEquals(T0, released.getUpdatedAt());
        assertEquals(T0, committed.getUpdatedAt());
        assertEquals(T0, restocked.getUpdatedAt());
    }

    @Test
    void releasingACommittedReservationIsRejected() {
        StockReservation reservation = reservationIn(ReservationStatus.COMMITTED);

        assertThrows(InvalidOperationException.class, () -> reservation.release(T1));
        assertEquals(ReservationStatus.COMMITTED, reservation.getStatus());
    }

    @Test
    void restockingAReservationThatNeverLeftIsRejected() {
        assertThrows(InvalidOperationException.class, () -> reservation().restock(T1));
    }

    @Test
    void committingAReleasedReservationIsRejected() {
        assertThrows(
            InvalidOperationException.class,
            () -> reservationIn(ReservationStatus.RELEASED).commit(T1)
        );
    }
}
