package io.d4tzz.ecommerce.cart.domain;

import static org.junit.jupiter.api.Assertions.*;

import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;

class CartItemTest {

    private static final VariantId VARIANT = VariantId.of(1L);

    // ---- creation ----

    @Test
    void create_startsWithoutAnIdAndKeepsTheQuantity() {
        CartItem item = CartItem.create(VARIANT, 3);

        assertNull(item.getId());
        assertEquals(VARIANT, item.getVariantId());
        assertEquals(3, item.getQuantity());
    }

    @Test
    void create_acceptsTheLimitsOfTheRange() {
        assertEquals(1, CartItem.create(VARIANT, 1).getQuantity());
        assertEquals(CartItem.MAX_QUANTITY, CartItem.create(VARIANT, CartItem.MAX_QUANTITY).getQuantity());
    }

    @Test
    void create_rejectsAQuantityOutsideTheRange() {
        for (int quantity : new int[] { 0, -1, CartItem.MAX_QUANTITY + 1 }) {
            assertThrows(InvalidArgumentException.class, () -> CartItem.create(VARIANT, quantity), "quantity " + quantity);
        }
    }

    @Test
    void restore_keepsTheIdAndStillChecksTheQuantity() {
        CartItem item = CartItem.restore(CartItemId.of(7L), VARIANT, 2);

        assertEquals(CartItemId.of(7L), item.getId());
        assertThrows(InvalidArgumentException.class, () -> CartItem.restore(CartItemId.of(7L), VARIANT, 0));
    }

    // ---- increaseQuantity ----

    @Test
    void increaseQuantity_addsToTheQuantity() {
        CartItem item = CartItem.create(VARIANT, 3);

        item.increaseQuantity(4);

        assertEquals(7, item.getQuantity());
    }

    @Test
    void increaseQuantity_canReachTheMaximumExactly() {
        CartItem item = CartItem.create(VARIANT, CartItem.MAX_QUANTITY - 2);

        item.increaseQuantity(2);

        assertEquals(CartItem.MAX_QUANTITY, item.getQuantity());
    }

    @Test
    void increaseQuantity_rejectsAnAmountThatIsNotPositive() {
        CartItem item = CartItem.create(VARIANT, 3);

        assertThrows(InvalidArgumentException.class, () -> item.increaseQuantity(0));
        assertThrows(InvalidArgumentException.class, () -> item.increaseQuantity(-1));
        assertEquals(3, item.getQuantity());
    }

    @Test
    void increaseQuantity_rejectsGoingAboveTheMaximumAndKeepsTheOldQuantity() {
        CartItem item = CartItem.create(VARIANT, CartItem.MAX_QUANTITY - 1);

        assertThrows(InvalidArgumentException.class, () -> item.increaseQuantity(2));
        assertEquals(CartItem.MAX_QUANTITY - 1, item.getQuantity());
    }

    // ---- changeQuantity ----

    @Test
    void changeQuantity_replacesTheQuantity() {
        CartItem item = CartItem.create(VARIANT, 3);

        item.changeQuantity(9);

        assertEquals(9, item.getQuantity());
    }

    @Test
    void changeQuantity_rejectsAQuantityOutsideTheRangeAndKeepsTheOldOne() {
        CartItem item = CartItem.create(VARIANT, 3);

        assertThrows(InvalidArgumentException.class, () -> item.changeQuantity(0));
        assertThrows(InvalidArgumentException.class, () -> item.changeQuantity(CartItem.MAX_QUANTITY + 1));
        assertEquals(3, item.getQuantity());
    }
}
