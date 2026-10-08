package io.d4tzz.ecommerce.order.application;

import static io.d4tzz.ecommerce.order.domain.OrderFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.inventory.application.ReleaseStockUseCase;
import io.d4tzz.ecommerce.inventory.application.command.ReleaseStockCommand;
import io.d4tzz.ecommerce.order.application.command.CancelMyOrderCommand;
import io.d4tzz.ecommerce.order.application.command.CancelOrderCommand;
import io.d4tzz.ecommerce.order.domain.Order;
import io.d4tzz.ecommerce.order.domain.OrderId;
import io.d4tzz.ecommerce.order.domain.OrderRepository;
import io.d4tzz.ecommerce.order.domain.OrderStatus;
import io.d4tzz.ecommerce.order.domain.event.OrderCancelledEvent;
import io.d4tzz.ecommerce.payment.application.RequestRefundUseCase;
import io.d4tzz.ecommerce.payment.application.command.RequestRefundCommand;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class CancelOrderUseCasesTest {

    private OrderRepository orderRepository;
    private ReleaseStockUseCase releaseStock;
    private RequestRefundUseCase requestRefund;
    private ApplicationEventPublisher events;
    private CancelMyOrderUseCase cancelMyOrder;
    private CancelOrderUseCase cancelOrder;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        releaseStock = mock(ReleaseStockUseCase.class);
        requestRefund = mock(RequestRefundUseCase.class);
        events = mock(ApplicationEventPublisher.class);
        cancelMyOrder = new CancelMyOrderUseCase(orderRepository, releaseStock, requestRefund, events);
        cancelOrder = new CancelOrderUseCase(orderRepository, releaseStock, requestRefund, events);
    }

    private void owns(Order order) {
        when(orderRepository.findByOrderCode(CODE)).thenReturn(Optional.of(order));
        when(orderRepository.findForUpdate(OrderId.of(ORDER_ID))).thenReturn(Optional.of(order));
    }

    private void exists(Order order) {
        when(orderRepository.findForUpdate(OrderId.of(ORDER_ID))).thenReturn(Optional.of(order));
    }

    private CancelMyOrderCommand myCommand() {
        return new CancelMyOrderCommand(USER_ID, CODE.value(), "Changed my mind");
    }

    // ---- customer ----

    @Test
    void myOrder_unpaidOrderIsCancelledAndStockReleasedWithoutRefund() {
        Order order = codOrder(OrderStatus.PENDING);
        owns(order);

        cancelMyOrder.execute(myCommand());

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(orderRepository).save(order);
        verify(releaseStock).execute(new ReleaseStockCommand(ORDER_ID));
        verifyNoInteractions(requestRefund);
        verify(events).publishEvent(any(OrderCancelledEvent.class));
    }

    @Test
    void myOrder_paidOrderAlsoRequestsRefundOfItsPayment() {
        Order order = prepaidPaid(OrderStatus.PENDING, "55");
        owns(order);

        cancelMyOrder.execute(myCommand());

        ArgumentCaptor<RequestRefundCommand> refund = ArgumentCaptor.forClass(RequestRefundCommand.class);
        verify(requestRefund).execute(refund.capture());
        assertEquals(55L, refund.getValue().paymentId());
    }

    @Test
    void myOrder_alreadyCancelledDoesNothing() {
        owns(codOrder(OrderStatus.CANCELLED));

        cancelMyOrder.execute(myCommand());

        verify(orderRepository, never()).save(any());
        verifyNoInteractions(releaseStock, requestRefund, events);
    }

    @Test
    void myOrder_unknownCodeIsNotFound() {
        when(orderRepository.findByOrderCode(CODE)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> cancelMyOrder.execute(myCommand()));
    }

    @Test
    void myOrder_someoneElsesOrderLooksMissing() {
        owns(codOrder(OrderStatus.PENDING));

        assertThrows(NotFoundException.class, () ->
            cancelMyOrder.execute(new CancelMyOrderCommand(USER_ID + 1, CODE.value(), "x"))
        );
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(releaseStock);
    }

    @Test
    void myOrder_confirmedOrderCannotBeCancelledByCustomer() {
        owns(codOrder(OrderStatus.CONFIRMED));

        assertThrows(InvalidOperationException.class, () -> cancelMyOrder.execute(myCommand()));
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(releaseStock, requestRefund, events);
    }

    // ---- admin ----

    @Test
    void admin_canCancelConfirmedOrder() {
        Order order = codOrder(OrderStatus.CONFIRMED);
        exists(order);

        cancelOrder.execute(new CancelOrderCommand(ORDER_ID, "Out of stock"));

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals("Out of stock", order.getCancelReason());
        verify(orderRepository).save(order);
        verify(releaseStock).execute(new ReleaseStockCommand(ORDER_ID));
        verifyNoInteractions(requestRefund);
        verify(events).publishEvent(any(OrderCancelledEvent.class));
    }

    @Test
    void admin_cancellingPaidOrderRequestsRefund() {
        exists(prepaidPaid(OrderStatus.CONFIRMED, "55"));

        cancelOrder.execute(new CancelOrderCommand(ORDER_ID, "Out of stock"));

        verify(requestRefund).execute(any(RequestRefundCommand.class));
    }

    @Test
    void admin_cannotCancelShippingOrder() {
        exists(codOrder(OrderStatus.SHIPPING));

        assertThrows(InvalidOperationException.class, () ->
            cancelOrder.execute(new CancelOrderCommand(ORDER_ID, "x"))
        );
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(releaseStock, requestRefund, events);
    }

    @Test
    void admin_blankReasonIsRejected() {
        exists(codOrder(OrderStatus.PENDING));

        assertThrows(InvalidArgumentException.class, () ->
            cancelOrder.execute(new CancelOrderCommand(ORDER_ID, " "))
        );
    }

    @Test
    void admin_alreadyCancelledDoesNothing() {
        exists(codOrder(OrderStatus.CANCELLED));

        cancelOrder.execute(new CancelOrderCommand(ORDER_ID, "again"));

        verify(orderRepository, never()).save(any());
        verifyNoInteractions(releaseStock, requestRefund, events);
    }

    @Test
    void admin_unknownOrderIsNotFound() {
        when(orderRepository.findForUpdate(OrderId.of(ORDER_ID))).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> cancelOrder.execute(new CancelOrderCommand(ORDER_ID, "x")));
    }
}
