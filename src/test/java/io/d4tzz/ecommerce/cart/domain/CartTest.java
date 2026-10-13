package io.d4tzz.ecommerce.cart.domain;

import static org.junit.jupiter.api.Assertions.*;

import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.user.domain.UserId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CartTest {

    private static final UserId USER = UserId.of(7L);
    private static final VariantId A = VariantId.of(1L);
    private static final VariantId B = VariantId.of(2L);

    private static Cart cart() {
        return Cart.create(USER);
    }

    private static int quantityOf(Cart cart, VariantId variant) {
        return cart.findItem(variant).orElseThrow().getQuantity();
    }

    // ---- creation ----

    @Test
    void create_isEmptyAndBelongsToTheUser() {
        Cart cart = cart();

        assertNull(cart.getId());
        assertEquals(USER, cart.getUserId());
        assertTrue(cart.isEmpty());
        assertEquals(0, cart.getTotalQuantity());
    }

    @Test
    void restoredCart_findsItsItemsByVariant() {
        Cart cart = new Cart(
            CartId.of(1L),
            USER,
            new ArrayList<>(List.of(CartItem.restore(CartItemId.of(10L), A, 2)))
        );

        assertEquals(2, quantityOf(cart, A));
        assertTrue(cart.findItem(B).isEmpty());
    }

    // ---- addItem ----

    @Test
    void addItem_addsANewItem() {
        Cart cart = cart();

        cart.addItem(A, 2);

        assertEquals(1, cart.getItems().size());
        assertEquals(2, quantityOf(cart, A));
    }

    @Test
    void addItem_addsTheQuantityToAnItemAlreadyInTheCart() {
        Cart cart = cart();
        cart.addItem(A, 2);

        cart.addItem(A, 3);

        assertEquals(1, cart.getItems().size());
        assertEquals(5, quantityOf(cart, A));
    }

    @Test
    void addItem_rejectsGoingAboveTheMaximumAndKeepsTheQuantity() {
        Cart cart = cart();
        cart.addItem(A, CartItem.MAX_QUANTITY);

        assertThrows(InvalidArgumentException.class, () -> cart.addItem(A, 1));
        assertEquals(CartItem.MAX_QUANTITY, quantityOf(cart, A));
    }

    @Test
    void addItem_rejectsAQuantityOutsideTheRange() {
        Cart cart = cart();

        assertThrows(InvalidArgumentException.class, () -> cart.addItem(A, 0));
        assertThrows(InvalidArgumentException.class, () -> cart.addItem(A, CartItem.MAX_QUANTITY + 1));
        assertTrue(cart.isEmpty());
    }

    @Test
    void addItem_keepsItemsInTheOrderTheyWereAdded() {
        Cart cart = cart();

        cart.addItem(B, 1);
        cart.addItem(A, 1);

        assertEquals(List.of(B, A), cart.getItems().stream().map(CartItem::getVariantId).toList());
    }

    // ---- updateQuantity ----

    @Test
    void updateQuantity_setsTheQuantityOfAnExistingItem() {
        Cart cart = cart();
        cart.addItem(A, 2);

        cart.updateQuantity(A, 9);

        assertEquals(9, quantityOf(cart, A));
    }

    @Test
    void updateQuantity_addsTheItemWhenItIsNotInTheCart() {
        Cart cart = cart();

        cart.updateQuantity(A, 4);

        assertEquals(4, quantityOf(cart, A));
    }

    @Test
    void updateQuantity_rejectsAQuantityOutsideTheRangeAndKeepsTheOldOne() {
        Cart cart = cart();
        cart.addItem(A, 2);

        assertThrows(InvalidArgumentException.class, () -> cart.updateQuantity(A, 0));
        assertThrows(InvalidArgumentException.class, () -> cart.updateQuantity(A, CartItem.MAX_QUANTITY + 1));
        assertEquals(2, quantityOf(cart, A));
    }

    // ---- removeItem / clear ----

    @Test
    void removeItem_removesOnlyThatVariant() {
        Cart cart = cart();
        cart.addItem(A, 1);
        cart.addItem(B, 1);

        cart.removeItem(A);

        assertTrue(cart.findItem(A).isEmpty());
        assertEquals(1, quantityOf(cart, B));
    }

    @Test
    void removeItem_doesNothingForAVariantNotInTheCart() {
        Cart cart = cart();
        cart.addItem(A, 1);

        cart.removeItem(B);

        assertEquals(1, cart.getItems().size());
    }

    @Test
    void removeItem_thenAddingTheSameVariantStartsFromTheNewQuantity() {
        Cart cart = cart();
        cart.addItem(A, 5);
        cart.removeItem(A);

        cart.addItem(A, 2);

        assertEquals(2, quantityOf(cart, A));
        assertEquals(1, cart.getItems().size());
    }

    @Test
    void clear_removesEveryItemAndTheCartCanBeUsedAgain() {
        Cart cart = cart();
        cart.addItem(A, 5);
        cart.addItem(B, 1);

        cart.clear();

        assertTrue(cart.isEmpty());
        assertTrue(cart.findItem(A).isEmpty());
        cart.addItem(A, 1);
        assertEquals(1, quantityOf(cart, A));
    }

    // ---- mergeItem ----

    @Test
    void mergeItem_addsAnItemTheCartDoesNotHave() {
        Cart cart = cart();

        cart.mergeItem(A, 3);

        assertEquals(3, quantityOf(cart, A));
    }

    @Test
    void mergeItem_keepsTheQuantityOfAnItemAlreadyInTheCart() {
        Cart cart = cart();
        cart.addItem(A, 2);

        cart.mergeItem(A, 8);

        assertEquals(2, quantityOf(cart, A));
    }

    @Test
    void mergeItem_lowersAQuantityAboveTheMaximumInsteadOfFailing() {
        Cart cart = cart();

        cart.mergeItem(A, 500);

        assertEquals(CartItem.MAX_QUANTITY, quantityOf(cart, A));
    }

    @Test
    void mergeItem_ignoresAQuantityThatIsNotPositive() {
        Cart cart = cart();

        cart.mergeItem(A, 0);
        cart.mergeItem(B, -2);

        assertTrue(cart.isEmpty());
    }

    // ---- totals ----

    @Test
    void getTotalQuantity_isTheSumOfAllItemQuantities() {
        Cart cart = cart();
        cart.addItem(A, 2);
        cart.addItem(B, 5);

        assertEquals(7, cart.getTotalQuantity());
        assertFalse(cart.isEmpty());
    }

    @Test
    void getItems_isUnmodifiable() {
        Cart cart = cart();
        cart.addItem(A, 1);

        assertThrows(UnsupportedOperationException.class, () -> cart.getItems().clear());
    }
}
