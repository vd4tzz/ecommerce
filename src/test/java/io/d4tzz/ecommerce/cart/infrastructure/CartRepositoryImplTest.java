package io.d4tzz.ecommerce.cart.infrastructure;

import static org.junit.jupiter.api.Assertions.*;

import io.d4tzz.ecommerce.cart.domain.Cart;
import io.d4tzz.ecommerce.cart.domain.CartId;
import io.d4tzz.ecommerce.cart.domain.CartItem;
import io.d4tzz.ecommerce.cart.infrastructure.persistence.repository.CartRepositoryImpl;
import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import io.d4tzz.ecommerce.support.PostgresContainerTest;
import io.d4tzz.ecommerce.user.domain.UserId;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(CartRepositoryImpl.class)
class CartRepositoryImplTest extends PostgresContainerTest {

    private static final UserId USER = UserId.of(7L);
    private static final VariantId A = VariantId.of(1L);
    private static final VariantId B = VariantId.of(2L);

    @Autowired CartRepositoryImpl repository;
    @Autowired EntityManager em;

    /** Writes pending changes and forgets them, so the next read comes from the database. */
    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    private Cart saveCartWith(UserId user, VariantId variant, int quantity) {
        Cart cart = Cart.create(user);
        cart.addItem(variant, quantity);
        return repository.save(cart);
    }

    private Cart reload(UserId user) {
        flushAndClear();
        return repository.findByUserId(user).orElseThrow();
    }

    private static int quantityOf(Cart cart, VariantId variant) {
        return cart.findItem(variant).orElseThrow().getQuantity();
    }

    // ---- save / find ----

    @Test
    void save_newEmptyCartGetsAnIdAndCanBeFoundByUser() {
        Cart saved = repository.save(Cart.create(USER));

        Cart loaded = reload(USER);

        assertNotNull(saved.getId());
        assertEquals(saved.getId(), loaded.getId());
        assertEquals(USER, loaded.getUserId());
        assertTrue(loaded.isEmpty());
    }

    @Test
    void save_newCartRoundTripsItsItems() {
        Cart cart = Cart.create(USER);
        cart.addItem(A, 2);
        cart.addItem(B, 5);
        repository.save(cart);

        Cart loaded = reload(USER);

        assertEquals(2, loaded.getItems().size());
        assertEquals(2, quantityOf(loaded, A));
        assertEquals(5, quantityOf(loaded, B));
        assertTrue(loaded.getItems().stream().allMatch(item -> item.getId() != null));
    }

    @Test
    void find_byIdReturnsTheSameCart() {
        Cart saved = saveCartWith(USER, A, 2);
        flushAndClear();

        Cart loaded = repository.find(saved.getId()).orElseThrow();

        assertEquals(USER, loaded.getUserId());
        assertEquals(2, quantityOf(loaded, A));
    }

    @Test
    void find_unknownIdOrUserIsEmpty() {
        assertTrue(repository.find(CartId.of(-1L)).isEmpty());
        assertTrue(repository.findByUserId(UserId.of(999L)).isEmpty());
    }

    @Test
    void findByUserId_returnsOnlyThatUsersCart() {
        saveCartWith(USER, A, 1);
        saveCartWith(UserId.of(8L), B, 4);

        Cart loaded = reload(UserId.of(8L));

        assertEquals(UserId.of(8L), loaded.getUserId());
        assertEquals(1, loaded.getItems().size());
        assertEquals(4, quantityOf(loaded, B));
    }

    // ---- updating a saved cart ----

    @Test
    void save_existingCartCanAddAnItem() {
        saveCartWith(USER, A, 2);
        Cart loaded = reload(USER);

        loaded.addItem(B, 3);
        repository.save(loaded);

        Cart reloaded = reload(USER);
        assertEquals(2, reloaded.getItems().size());
        assertEquals(3, quantityOf(reloaded, B));
    }

    @Test
    void save_existingCartCanChangeAQuantity() {
        saveCartWith(USER, A, 2);
        Cart loaded = reload(USER);
        CartItem before = loaded.findItem(A).orElseThrow();

        loaded.updateQuantity(A, 9);
        repository.save(loaded);

        Cart reloaded = reload(USER);
        assertEquals(9, quantityOf(reloaded, A));
        assertEquals(before.getId(), reloaded.findItem(A).orElseThrow().getId());
    }

    @Test
    void save_existingCartCanRemoveAnItem() {
        Cart cart = Cart.create(USER);
        cart.addItem(A, 1);
        cart.addItem(B, 1);
        repository.save(cart);
        Cart loaded = reload(USER);

        loaded.removeItem(A);
        repository.save(loaded);

        Cart reloaded = reload(USER);
        assertEquals(List.of(B), reloaded.getItems().stream().map(CartItem::getVariantId).toList());
    }

    @Test
    void save_clearedCartHasNoItemsButStillExists() {
        Cart saved = saveCartWith(USER, A, 2);
        Cart loaded = reload(USER);

        loaded.clear();
        repository.save(loaded);

        Cart reloaded = reload(USER);
        assertEquals(saved.getId(), reloaded.getId());
        assertTrue(reloaded.isEmpty());
    }

    // ---- constraints and delete ----

    @Test
    void save_aSecondCartForTheSameUserViolatesTheUniqueConstraint() {
        repository.save(Cart.create(USER));
        em.flush();

        assertThrows(ConstraintViolationException.class, () -> {
            repository.save(Cart.create(USER));
            em.flush();
        });
    }

    @Test
    void delete_removesTheCartAndItsItems() {
        Cart saved = saveCartWith(USER, A, 2);
        flushAndClear();

        repository.delete(saved.getId());
        flushAndClear();

        assertTrue(repository.find(saved.getId()).isEmpty());
        assertEquals(Optional.empty(), repository.findByUserId(USER));
    }

    // ---- locking ----

    @Test
    void lockForUser_worksBeforeTheCartExistsAndCanBeCalledAgainInTheSameTransaction() {
        assertDoesNotThrow(() -> {
            repository.lockForUser(USER);
            repository.lockForUser(USER);
        });
    }
}
