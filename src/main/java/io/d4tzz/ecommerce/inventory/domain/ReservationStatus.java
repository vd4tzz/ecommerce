package io.d4tzz.ecommerce.inventory.domain;

/**
 * Where a reservation is in its life: {@code RESERVED -> RELEASED} or {@code RESERVED -> COMMITTED -> RESTOCKED}.
 * RELEASED and RESTOCKED are final.
 */
public enum ReservationStatus {
    /** Stock is held for the order. */
    RESERVED,
    /** Hold released before the goods left the warehouse (order cancelled/expired). */
    RELEASED,
    /** Goods left the warehouse. */
    COMMITTED,
    /** Shipped goods came back into the warehouse. */
    RESTOCKED;

    /** Whether the reservation may move from this status to {@code next}. */
    public boolean canTransitionTo(ReservationStatus next) {
        return switch (this) {
            case RESERVED -> next == RELEASED || next == COMMITTED;
            case COMMITTED -> next == RESTOCKED;
            case RELEASED, RESTOCKED -> false;
        };
    }
}
