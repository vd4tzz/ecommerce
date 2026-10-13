package io.d4tzz.ecommerce.inventory.domain;

import static org.junit.jupiter.api.Assertions.*;

import io.d4tzz.ecommerce.common.Address;
import io.d4tzz.ecommerce.common.exception.ConflictException;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class WarehouseTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = T0.plusSeconds(60);
    private static final Address ADDRESS = Address.of("Ha Noi", "Cau Giay", "Dich Vong", "1 Xuan Thuy");

    private static Warehouse warehouse() {
        return Warehouse.create(WarehouseCode.of("HN-01"), "Hanoi", ADDRESS, code -> false, T0);
    }

    // ---- create ----

    @Test
    void create_startsActiveWithoutAnId() {
        Warehouse warehouse = warehouse();

        assertNull(warehouse.getId());
        assertEquals(WarehouseCode.of("HN-01"), warehouse.getCode());
        assertEquals("Hanoi", warehouse.getName());
        assertEquals(ADDRESS, warehouse.getAddress());
        assertEquals(WarehouseStatus.ACTIVE, warehouse.getStatus());
        assertTrue(warehouse.isActive());
        assertEquals(T0, warehouse.getCreatedAt());
        assertEquals(T0, warehouse.getUpdatedAt());
    }

    @Test
    void create_failsWhenTheCodeIsTaken() {
        assertThrows(
            ConflictException.class,
            () -> Warehouse.create(WarehouseCode.of("HN-01"), "Hanoi", ADDRESS, code -> true, T0)
        );
    }

    @Test
    void create_trimsTheName() {
        Warehouse warehouse = Warehouse.create(WarehouseCode.of("HN-01"), "  Hanoi  ", ADDRESS, code -> false, T0);

        assertEquals("Hanoi", warehouse.getName());
    }

    @Test
    void create_rejectsABlankName() {
        assertThrows(
            InvalidArgumentException.class,
            () -> Warehouse.create(WarehouseCode.of("HN-01"), "   ", ADDRESS, code -> false, T0)
        );
    }

    @Test
    void create_acceptsAName150CharactersLongButNotLonger() {
        assertDoesNotThrow(() -> Warehouse.create(WarehouseCode.of("HN-01"), "x".repeat(150), ADDRESS, code -> false, T0));
        assertThrows(
            InvalidArgumentException.class,
            () -> Warehouse.create(WarehouseCode.of("HN-01"), "x".repeat(151), ADDRESS, code -> false, T0)
        );
    }

    // ---- update ----

    @Test
    void update_changesNameAndAddressButNotTheCode() {
        Warehouse warehouse = warehouse();
        Address other = Address.of("Da Nang", "Hai Chau", "Thach Thang", "2 Bach Dang");

        warehouse.update("Danang", other, T1);

        assertEquals("Danang", warehouse.getName());
        assertEquals(other, warehouse.getAddress());
        assertEquals(WarehouseCode.of("HN-01"), warehouse.getCode());
        assertEquals(T1, warehouse.getUpdatedAt());
    }

    @Test
    void update_rejectsABlankNameAndKeepsTheOldOne() {
        Warehouse warehouse = warehouse();

        assertThrows(InvalidArgumentException.class, () -> warehouse.update(" ", ADDRESS, T1));
        assertEquals("Hanoi", warehouse.getName());
    }

    // ---- status ----

    @Test
    void deactivate_makesItInactive() {
        Warehouse warehouse = warehouse();

        assertTrue(warehouse.deactivate(T1));

        assertEquals(WarehouseStatus.INACTIVE, warehouse.getStatus());
        assertFalse(warehouse.isActive());
        assertEquals(T1, warehouse.getUpdatedAt());
    }

    @Test
    void deactivate_onAnInactiveWarehouseChangesNothing() {
        Warehouse warehouse = warehouse();
        warehouse.deactivate(T1);

        assertFalse(warehouse.deactivate(T1.plusSeconds(1)));
        assertEquals(T1, warehouse.getUpdatedAt());
    }

    @Test
    void activate_makesItActiveAgain() {
        Warehouse warehouse = warehouse();
        warehouse.deactivate(T1);

        assertTrue(warehouse.activate(T1.plusSeconds(1)));

        assertTrue(warehouse.isActive());
    }

    @Test
    void activate_onAnActiveWarehouseChangesNothing() {
        Warehouse warehouse = warehouse();

        assertFalse(warehouse.activate(T1));
        assertEquals(T0, warehouse.getUpdatedAt());
    }

    @Test
    void ensureActive_failsOnlyWhenInactive() {
        Warehouse warehouse = warehouse();
        assertDoesNotThrow(warehouse::ensureActive);

        warehouse.deactivate(T1);

        assertThrows(InvalidOperationException.class, warehouse::ensureActive);
    }
}
