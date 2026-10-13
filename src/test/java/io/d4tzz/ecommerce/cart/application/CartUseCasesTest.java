package io.d4tzz.ecommerce.cart.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.d4tzz.ecommerce.cart.application.command.AddToCartCommand;
import io.d4tzz.ecommerce.cart.application.command.ClearCartCommand;
import io.d4tzz.ecommerce.cart.application.command.MergeCartCommand;
import io.d4tzz.ecommerce.cart.application.command.RemoveCartItemCommand;
import io.d4tzz.ecommerce.cart.application.command.UpdateCartItemCommand;
import io.d4tzz.ecommerce.cart.domain.Cart;
import io.d4tzz.ecommerce.cart.domain.CartItem;
import io.d4tzz.ecommerce.cart.domain.CartRepository;
import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import io.d4tzz.ecommerce.catalog.published.VariantSaleChecker;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.user.domain.UserId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

class CartUseCasesTest {

    private static final long USER_ID = 7L;
    private static final UserId USER = UserId.of(USER_ID);
    private static final long A = 1L;
    private static final long B = 2L;

    private CartRepository repository;
    private VariantSaleChecker saleChecker;
    private AddToCartUseCase addToCart;
    private UpdateCartItemUseCase updateItem;
    private RemoveCartItemUseCase removeItem;
    private ClearCartUseCase clearCart;
    private MergeCartUseCase mergeCart;

    @BeforeEach
    void setUp() {
        repository = mock(CartRepository.class);
        saleChecker = mock(VariantSaleChecker.class);
        addToCart = new AddToCartUseCase(repository, saleChecker);
        updateItem = new UpdateCartItemUseCase(repository, saleChecker);
        removeItem = new RemoveCartItemUseCase(repository);
        clearCart = new ClearCartUseCase(repository);
        mergeCart = new MergeCartUseCase(repository, saleChecker);
    }

    private Cart existingCart(long variantId, int quantity) {
        Cart cart = Cart.create(USER);
        cart.addItem(VariantId.of(variantId), quantity);
        when(repository.findByUserId(USER)).thenReturn(Optional.of(cart));
        return cart;
    }

    private void userHasNoCart() {
        when(repository.findByUserId(USER)).thenReturn(Optional.empty());
    }

    private void notForSale(long... variantIds) {
        when(saleChecker.checkNotForSale(any())).thenReturn(
            java.util.Arrays.stream(variantIds).mapToObj(VariantId::of).collect(java.util.stream.Collectors.toSet())
        );
    }

    private Cart savedCart() {
        ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    private static int quantityOf(Cart cart, long variantId) {
        return cart.findItem(VariantId.of(variantId)).orElseThrow().getQuantity();
    }

    // ---- add to cart ----

    @Test
    void add_addsTheVariantToTheExistingCart() {
        Cart cart = existingCart(A, 2);

        addToCart.execute(new AddToCartCommand(USER_ID, A, 3));

        assertEquals(5, quantityOf(cart, A));
        verify(repository).save(cart);
    }

    @Test
    void add_createsTheCartWhenTheUserHasNone() {
        userHasNoCart();

        addToCart.execute(new AddToCartCommand(USER_ID, A, 2));

        Cart saved = savedCart();
        assertEquals(USER, saved.getUserId());
        assertEquals(2, quantityOf(saved, A));
    }

    @Test
    void add_locksTheUsersCartBeforeReadingIt() {
        userHasNoCart();

        addToCart.execute(new AddToCartCommand(USER_ID, A, 1));

        InOrder order = inOrder(repository);
        order.verify(repository).lockForUser(USER);
        order.verify(repository).findByUserId(USER);
        order.verify(repository).save(any(Cart.class));
    }

    @Test
    void add_rejectsAVariantThatIsNotForSaleBeforeTouchingTheCart() {
        notForSale(A);

        assertThrows(InvalidOperationException.class, () -> addToCart.execute(new AddToCartCommand(USER_ID, A, 1)));
        verifyNoInteractions(repository);
    }

    @Test
    void add_rejectsAQuantityThatWouldExceedTheMaximumAndSavesNothing() {
        existingCart(A, CartItem.MAX_QUANTITY);

        assertThrows(InvalidArgumentException.class, () -> addToCart.execute(new AddToCartCommand(USER_ID, A, 1)));
        verify(repository, never()).save(any());
    }

    // ---- update item ----

    @Test
    void update_setsTheQuantityOfAnItemInTheCart() {
        Cart cart = existingCart(A, 2);

        updateItem.execute(new UpdateCartItemCommand(USER_ID, A, 9));

        assertEquals(9, quantityOf(cart, A));
        verify(repository).save(cart);
    }

    @Test
    void update_addsTheVariantAndTheCartWhenMissing() {
        userHasNoCart();

        updateItem.execute(new UpdateCartItemCommand(USER_ID, A, 4));

        assertEquals(4, quantityOf(savedCart(), A));
    }

    @Test
    void update_rejectsAVariantThatIsNotForSaleBeforeTouchingTheCart() {
        notForSale(A);

        assertThrows(InvalidOperationException.class, () ->
            updateItem.execute(new UpdateCartItemCommand(USER_ID, A, 1))
        );
        verifyNoInteractions(repository);
    }

    @Test
    void update_rejectsAQuantityOutsideTheRangeAndSavesNothing() {
        existingCart(A, 2);

        assertThrows(InvalidArgumentException.class, () ->
            updateItem.execute(new UpdateCartItemCommand(USER_ID, A, 0))
        );
        verify(repository, never()).save(any());
    }

    // ---- remove item ----

    @Test
    void remove_removesTheVariantAndSaves() {
        Cart cart = existingCart(A, 2);
        cart.addItem(VariantId.of(B), 1);

        removeItem.execute(new RemoveCartItemCommand(USER_ID, A));

        assertTrue(cart.findItem(VariantId.of(A)).isEmpty());
        assertEquals(1, quantityOf(cart, B));
        verify(repository).save(cart);
    }

    @Test
    void remove_doesNothingWhenTheUserHasNoCart() {
        userHasNoCart();

        removeItem.execute(new RemoveCartItemCommand(USER_ID, A));

        verify(repository).lockForUser(USER);
        verify(repository, never()).save(any());
    }

    // ---- clear cart ----

    @Test
    void clear_emptiesTheCartAndSaves() {
        Cart cart = existingCart(A, 2);

        clearCart.execute(new ClearCartCommand(USER_ID));

        assertTrue(cart.isEmpty());
        verify(repository).save(cart);
    }

    @Test
    void clear_doesNothingWhenTheUserHasNoCart() {
        userHasNoCart();

        clearCart.execute(new ClearCartCommand(USER_ID));

        verify(repository, never()).save(any());
    }

    // ---- merge cart ----

    @Test
    void merge_addsTheGuestItemsToTheUsersCart() {
        Cart cart = existingCart(A, 2);

        mergeCart.execute(
            new MergeCartCommand(USER_ID, List.of(new MergeCartCommand.Item(B, 3), new MergeCartCommand.Item(A, 8)))
        );

        assertEquals(2, quantityOf(cart, A)); // already in the cart: keeps its quantity
        assertEquals(3, quantityOf(cart, B));
        verify(repository).save(cart);
    }

    @Test
    void merge_createsTheCartWhenTheUserHasNone() {
        userHasNoCart();

        mergeCart.execute(new MergeCartCommand(USER_ID, List.of(new MergeCartCommand.Item(A, 1))));

        Cart saved = savedCart();
        assertEquals(USER, saved.getUserId());
        assertEquals(1, quantityOf(saved, A));
    }

    @Test
    void merge_skipsVariantsThatAreNotForSaleInsteadOfFailing() {
        userHasNoCart();
        notForSale(A);

        mergeCart.execute(
            new MergeCartCommand(USER_ID, List.of(new MergeCartCommand.Item(A, 1), new MergeCartCommand.Item(B, 2)))
        );

        Cart saved = savedCart();
        assertTrue(saved.findItem(VariantId.of(A)).isEmpty());
        assertEquals(2, quantityOf(saved, B));
    }

    @Test
    void merge_lowersAQuantityAboveTheMaximum() {
        userHasNoCart();

        mergeCart.execute(new MergeCartCommand(USER_ID, List.of(new MergeCartCommand.Item(A, 500))));

        assertEquals(CartItem.MAX_QUANTITY, quantityOf(savedCart(), A));
    }

    @Test
    void merge_asksTheCatalogAboutEveryGuestVariantInOneCall() {
        userHasNoCart();

        mergeCart.execute(
            new MergeCartCommand(USER_ID, List.of(new MergeCartCommand.Item(A, 1), new MergeCartCommand.Item(B, 1)))
        );

        verify(saleChecker, times(1)).checkNotForSale(List.of(VariantId.of(A), VariantId.of(B)));
    }

    @Test
    void merge_withNoItemsStillSavesTheCart() {
        userHasNoCart();

        mergeCart.execute(new MergeCartCommand(USER_ID, List.of()));

        assertTrue(savedCart().isEmpty());
    }
}
