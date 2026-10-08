package io.d4tzz.ecommerce.order.application;

import static io.d4tzz.ecommerce.order.domain.OrderFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.inventory.application.ReleaseStockUseCase;
import io.d4tzz.ecommerce.inventory.application.command.ReleaseStockCommand;
import io.d4tzz.ecommerce.order.application.command.PlaceOrderCommand;
import io.d4tzz.ecommerce.order.domain.Order;
import io.d4tzz.ecommerce.order.domain.OrderId;
import io.d4tzz.ecommerce.order.domain.OrderRepository;
import io.d4tzz.ecommerce.order.domain.OrderStatus;
import io.d4tzz.ecommerce.order.domain.PaymentStatus;
import io.d4tzz.ecommerce.order.domain.event.OrderCancelledEvent;
import io.d4tzz.ecommerce.order.domain.event.OrderPlacedEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class PlaceAndExpireOrdersUseCasesTest {

    private OrderRepository orderRepository;
    private ApplicationEventPublisher events;
    private ReleaseStockUseCase releaseStock;
    private TransactionTemplate transactionTemplate;

    private PlaceOrderUseCase placeOrder;
    private ExpireOrdersUseCase expireOrders;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        events = mock(ApplicationEventPublisher.class);
        releaseStock = mock(ReleaseStockUseCase.class);
        transactionTemplate = mock(TransactionTemplate.class);
        // Run the callback directly; a RuntimeException from it propagates like a rolled back transaction
        when(transactionTemplate.execute(any())).thenAnswer(call ->
            ((TransactionCallback<Object>) call.getArgument(0)).doInTransaction(null)
        );
        placeOrder = new PlaceOrderUseCase(orderRepository, events);
        expireOrders = new ExpireOrdersUseCase(orderRepository, releaseStock, events, transactionTemplate);
        when(orderRepository.save(any())).thenAnswer(call -> {
            Order saved = call.getArgument(0);
            return Order.restore(
                OrderId.of(ORDER_ID), saved.getOrderCode(), saved.getUserId(), saved.getRecipient(),
                saved.getShippingAddress(), saved.getItems(), saved.getShippingFee(), saved.getTotalAmount(),
                saved.getStatus(), saved.getPaymentStatus(), saved.getNote(), saved.getPaymentExpiresAt(),
                saved.getPaidAt(), saved.getPaymentReference(), saved.getCancelReason(), saved.getCancelledAt(),
                saved.getCompletedAt(), saved.getCreatedAt(), saved.getUpdatedAt()
            );
        });
    }

    // ---- place ----

    private PlaceOrderCommand command(PlaceOrderCommand.PaymentMode mode, Instant paymentExpiresAt, String unitPrice) {
        return new PlaceOrderCommand(
            USER_ID,
            "Jane Doe",
            "0901234567",
            "Ha Noi",
            "Cau Giay",
            "Dich Vong",
            "1 Xuan Thuy",
            List.of(new PlaceOrderCommand.Item(1L, 10L, "Shirt", "SKU-1", "M / Red", null, new BigDecimal(unitPrice), 2)),
            new BigDecimal("30000"),
            mode,
            paymentExpiresAt,
            null
        );
    }

    @Test
    void place_codOrderIsPendingUnpaidWithoutDeadline() {
        Long id = placeOrder.execute(command(PlaceOrderCommand.PaymentMode.COD, null, "100000"));

        assertEquals(ORDER_ID, id);
        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(saved.capture());
        assertEquals(OrderStatus.PENDING, saved.getValue().getStatus());
        assertEquals(PaymentStatus.UNPAID, saved.getValue().getPaymentStatus());
        assertFalse(saved.getValue().requiresPrepayment());
        assertEquals(TOTAL, saved.getValue().getTotalAmount());
        verify(events).publishEvent(any(OrderPlacedEvent.class));
    }

    @Test
    void place_codIgnoresPaymentExpiry() {
        placeOrder.execute(command(PlaceOrderCommand.PaymentMode.COD, Instant.now().plusSeconds(60), "100000"));

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(saved.capture());
        assertNull(saved.getValue().getPaymentExpiresAt());
    }

    @Test
    void place_prepaidOrderKeepsItsPaymentDeadline() {
        Instant deadline = Instant.now().plusSeconds(900);

        placeOrder.execute(command(PlaceOrderCommand.PaymentMode.PREPAID, deadline, "100000"));

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(saved.capture());
        assertEquals(deadline, saved.getValue().getPaymentExpiresAt());
        assertTrue(saved.getValue().requiresPrepayment());
    }

    @Test
    void place_prepaidWithoutDeadlineIsRejected() {
        assertThrows(InvalidArgumentException.class, () ->
            placeOrder.execute(command(PlaceOrderCommand.PaymentMode.PREPAID, null, "100000"))
        );
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void place_prepaidWithDeadlineInThePastIsRejected() {
        assertThrows(InvalidArgumentException.class, () ->
            placeOrder.execute(command(PlaceOrderCommand.PaymentMode.PREPAID, Instant.now().minusSeconds(1), "100000"))
        );
        verify(orderRepository, never()).save(any());
    }

    @Test
    void place_invalidPhoneNumberIsRejected() {
        PlaceOrderCommand base = command(PlaceOrderCommand.PaymentMode.COD, null, "100000");
        PlaceOrderCommand badPhone = new PlaceOrderCommand(
            base.userId(), base.recipientName(), "abc", base.province(), base.district(), base.ward(),
            base.detailAddress(), base.items(), base.shippingFee(), base.paymentMode(), null, null
        );

        assertThrows(InvalidArgumentException.class, () -> placeOrder.execute(badPhone));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void place_freeOrderIsPaidFromTheStart() {
        PlaceOrderCommand base = command(PlaceOrderCommand.PaymentMode.COD, null, "0");
        PlaceOrderCommand free = new PlaceOrderCommand(
            base.userId(), base.recipientName(), base.recipientPhone(), base.province(), base.district(), base.ward(),
            base.detailAddress(), base.items(), BigDecimal.ZERO, base.paymentMode(), null, null
        );

        placeOrder.execute(free);

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(saved.capture());
        assertEquals(PaymentStatus.PAID, saved.getValue().getPaymentStatus());
    }

    // ---- expire ----

    @Test
    void expire_cancelsEachOverdueOrderReleasesStockAndCountsThem() {
        Order first = prepaidUnpaid(OrderStatus.PENDING);
        Order second = Order.restore(
            OrderId.of(2L), CODE, first.getUserId(), first.getRecipient(), first.getShippingAddress(), first.getItems(),
            first.getShippingFee(), first.getTotalAmount(), OrderStatus.PENDING, PaymentStatus.UNPAID, null, PAY_BY,
            null, null, null, null, null, NOW, NOW
        );
        when(orderRepository.findPaymentExpiredIds(any())).thenReturn(List.of(OrderId.of(1L), OrderId.of(2L)));
        when(orderRepository.findForUpdate(OrderId.of(1L))).thenReturn(Optional.of(first));
        when(orderRepository.findForUpdate(OrderId.of(2L))).thenReturn(Optional.of(second));

        int expired = expireOrders.execute();

        assertEquals(2, expired);
        assertEquals(OrderStatus.CANCELLED, first.getStatus());
        assertEquals(Order.PAYMENT_EXPIRED_REASON, first.getCancelReason());
        verify(releaseStock).execute(new ReleaseStockCommand(1L));
        verify(releaseStock).execute(new ReleaseStockCommand(2L));
        verify(events, times(2)).publishEvent(any(OrderCancelledEvent.class));
    }

    @Test
    void expire_orderPaidSinceTheSearchIsLeftAlone() {
        when(orderRepository.findPaymentExpiredIds(any())).thenReturn(List.of(OrderId.of(1L)));
        when(orderRepository.findForUpdate(OrderId.of(1L))).thenReturn(Optional.of(prepaidPaid(OrderStatus.PENDING, "55")));

        assertEquals(0, expireOrders.execute());
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(releaseStock, events);
    }

    @Test
    void expire_orderDeletedSinceTheSearchIsSkipped() {
        when(orderRepository.findPaymentExpiredIds(any())).thenReturn(List.of(OrderId.of(1L)));
        when(orderRepository.findForUpdate(OrderId.of(1L))).thenReturn(Optional.empty());

        assertEquals(0, expireOrders.execute());
    }

    @Test
    void expire_oneFailingOrderDoesNotStopTheOthers() {
        Order second = prepaidUnpaid(OrderStatus.PENDING);
        when(orderRepository.findPaymentExpiredIds(any())).thenReturn(List.of(OrderId.of(1L), OrderId.of(2L)));
        when(orderRepository.findForUpdate(OrderId.of(1L))).thenThrow(new IllegalStateException("db down"));
        when(orderRepository.findForUpdate(OrderId.of(2L))).thenReturn(Optional.of(second));

        int expired = expireOrders.execute();

        assertEquals(1, expired);
        assertEquals(OrderStatus.CANCELLED, second.getStatus());
        verify(releaseStock).execute(new ReleaseStockCommand(ORDER_ID));
    }

    @Test
    void expire_nothingOverdueDoesNothing() {
        when(orderRepository.findPaymentExpiredIds(any())).thenReturn(List.of());

        assertEquals(0, expireOrders.execute());
        verifyNoInteractions(releaseStock, events);
    }
}
