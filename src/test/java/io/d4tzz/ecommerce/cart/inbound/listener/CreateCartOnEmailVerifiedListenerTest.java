package io.d4tzz.ecommerce.cart.inbound.listener;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.d4tzz.ecommerce.cart.domain.Cart;
import io.d4tzz.ecommerce.cart.domain.CartId;
import io.d4tzz.ecommerce.cart.domain.CartRepository;
import io.d4tzz.ecommerce.user.domain.Email;
import io.d4tzz.ecommerce.user.domain.UserId;
import io.d4tzz.ecommerce.user.domain.event.EmailVerifiedEvent;
import java.util.ArrayList;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

class CreateCartOnEmailVerifiedListenerTest {

    private static final UserId USER = UserId.of(7L);

    private CartRepository repository;
    private CreateCartOnEmailVerifiedListener listener;

    @BeforeEach
    void setUp() {
        repository = mock(CartRepository.class);
        listener = new CreateCartOnEmailVerifiedListener(repository);
    }

    private static EmailVerifiedEvent event() {
        return new EmailVerifiedEvent(USER, Email.of("user@example.com"));
    }

    @Test
    void createsAnEmptyCartForTheUser() {
        when(repository.findByUserId(USER)).thenReturn(Optional.empty());

        listener.onEmailVerified(event());

        ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(repository).save(captor.capture());
        assertEquals(USER, captor.getValue().getUserId());
        assertTrue(captor.getValue().isEmpty());
    }

    @Test
    void doesNotCreateASecondCartWhenTheEventArrivesAgain() {
        when(repository.findByUserId(USER)).thenReturn(
            Optional.of(new Cart(CartId.of(1L), USER, new ArrayList<>()))
        );

        listener.onEmailVerified(event());

        verify(repository, never()).save(any());
    }

    @Test
    void locksTheUsersCartBeforeLookingForIt() {
        when(repository.findByUserId(USER)).thenReturn(Optional.empty());

        listener.onEmailVerified(event());

        InOrder order = inOrder(repository);
        order.verify(repository).lockForUser(USER);
        order.verify(repository).findByUserId(USER);
    }
}
