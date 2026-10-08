package io.d4tzz.ecommerce.order.domain;

import static io.d4tzz.ecommerce.order.domain.OrderStatus.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OrderStatusTest {

    @Test
    void transitions_followTheFulfillmentFlow() {
        assertTrue(PENDING.canTransitionTo(CONFIRMED));
        assertTrue(PENDING.canTransitionTo(CANCELLED));
        assertTrue(CONFIRMED.canTransitionTo(SHIPPING));
        assertTrue(CONFIRMED.canTransitionTo(CANCELLED));
        assertTrue(SHIPPING.canTransitionTo(DELIVERED));
        assertTrue(SHIPPING.canTransitionTo(RETURNED));
    }

    @Test
    void transitions_cannotSkipOrGoBack() {
        assertFalse(PENDING.canTransitionTo(SHIPPING));
        assertFalse(PENDING.canTransitionTo(DELIVERED));
        assertFalse(CONFIRMED.canTransitionTo(PENDING));
        assertFalse(CONFIRMED.canTransitionTo(DELIVERED));
        assertFalse(SHIPPING.canTransitionTo(CANCELLED));
        assertFalse(SHIPPING.canTransitionTo(CONFIRMED));
    }

    @Test
    void finalStatuses_haveNoWayOut() {
        for (OrderStatus from : new OrderStatus[] { DELIVERED, RETURNED, CANCELLED }) {
            assertTrue(from.isFinal());
            for (OrderStatus to : OrderStatus.values()) {
                assertFalse(from.canTransitionTo(to), from + " -> " + to);
            }
        }
        assertFalse(PENDING.isFinal());
        assertFalse(CONFIRMED.isFinal());
        assertFalse(SHIPPING.isFinal());
    }
}
