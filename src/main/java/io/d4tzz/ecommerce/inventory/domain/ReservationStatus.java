package io.d4tzz.ecommerce.inventory.domain;

public enum ReservationStatus {
    /** Stock is held for the order. */
    RESERVED,
    /** Hold released before the goods left the warehouse (order cancelled/expired). */
    RELEASED,
    /** Goods left the warehouse. */
    COMMITTED,
    /** Shipped goods came back into the warehouse. */
    RESTOCKED;

    public boolean canTransitionTo(ReservationStatus next) {
        return switch (this) {
            case RESERVED -> next == RELEASED || next == COMMITTED;
            case COMMITTED -> next == RESTOCKED;
            case RELEASED, RESTOCKED -> false;
        };
    }
}
