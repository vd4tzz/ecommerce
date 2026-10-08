package io.d4tzz.ecommerce.order.application;

import static io.d4tzz.ecommerce.order.domain.OrderFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.inventory.application.CommitStockUseCase;
import io.d4tzz.ecommerce.inventory.application.RestockReturnedUseCase;
import io.d4tzz.ecommerce.inventory.application.command.CommitStockCommand;
import io.d4tzz.ecommerce.inventory.application.command.RestockReturnedCommand;
import io.d4tzz.ecommerce.order.application.command.ConfirmOrderCommand;
import io.d4tzz.ecommerce.order.application.command.DeliverOrderCommand;
import io.d4tzz.ecommerce.order.application.command.ReturnOrderCommand;
import io.d4tzz.ecommerce.order.application.command.ShipOrderCommand;
import io.d4tzz.ecommerce.order.domain.Order;
import io.d4tzz.ecommerce.order.domain.OrderId;
import io.d4tzz.ecommerce.order.domain.OrderRepository;
import io.d4tzz.ecommerce.order.domain.OrderStatus;
import io.d4tzz.ecommerce.order.domain.event.OrderConfirmedEvent;
import io.d4tzz.ecommerce.order.domain.event.OrderDeliveredEvent;
import io.d4tzz.ecommerce.order.domain.event.OrderReturnedEvent;
import io.d4tzz.ecommerce.order.domain.event.OrderShippedEvent;
import io.d4tzz.ecommerce.payment.application.RecordPaymentUseCase;
import io.d4tzz.ecommerce.payment.application.RequestRefundUseCase;
import io.d4tzz.ecommerce.payment.application.command.RecordPaymentCommand;
import io.d4tzz.ecommerce.payment.application.command.RequestRefundCommand;
import io.d4tzz.ecommerce.payment.domain.PaymentMethod;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class FulfillmentUseCasesTest {

    private OrderRepository orderRepository;
    private ApplicationEventPublisher events;
    private CommitStockUseCase commitStock;
    private RestockReturnedUseCase restockReturned;
    private RecordPaymentUseCase recordPayment;
    private RequestRefundUseCase requestRefund;

    private ConfirmOrderUseCase confirm;
    private ShipOrderUseCase ship;
    private DeliverOrderUseCase deliver;
    private ReturnOrderUseCase returnOrder;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        events = mock(ApplicationEventPublisher.class);
        commitStock = mock(CommitStockUseCase.class);
        restockReturned = mock(RestockReturnedUseCase.class);
        recordPayment = mock(RecordPaymentUseCase.class);
        requestRefund = mock(RequestRefundUseCase.class);
        confirm = new ConfirmOrderUseCase(orderRepository, events);
        ship = new ShipOrderUseCase(orderRepository, commitStock, events);
        deliver = new DeliverOrderUseCase(orderRepository, recordPayment, events);
        returnOrder = new ReturnOrderUseCase(orderRepository, restockReturned, requestRefund, events);
    }

    private void exists(Order order) {
        when(orderRepository.findForUpdate(OrderId.of(ORDER_ID))).thenReturn(Optional.of(order));
    }

    // ---- confirm ----

    @Test
    void confirm_movesOrderToConfirmedAndPublishesEvent() {
        Order order = codOrder(OrderStatus.PENDING);
        exists(order);

        confirm.execute(new ConfirmOrderCommand(ORDER_ID));

        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        verify(orderRepository).save(order);
        verify(events).publishEvent(any(OrderConfirmedEvent.class));
    }

    @Test
    void confirm_unpaidPrepaidOrderIsRejected() {
        exists(prepaidUnpaid(OrderStatus.PENDING));

        assertThrows(InvalidOperationException.class, () -> confirm.execute(new ConfirmOrderCommand(ORDER_ID)));
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void confirm_alreadyConfirmedDoesNothing() {
        exists(codOrder(OrderStatus.CONFIRMED));

        confirm.execute(new ConfirmOrderCommand(ORDER_ID));

        verify(orderRepository, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void confirm_unknownOrderIsNotFound() {
        when(orderRepository.findForUpdate(OrderId.of(ORDER_ID))).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> confirm.execute(new ConfirmOrderCommand(ORDER_ID)));
    }

    // ---- ship ----

    @Test
    void ship_commitsReservedStockAndPublishesEvent() {
        Order order = codOrder(OrderStatus.CONFIRMED);
        exists(order);

        ship.execute(new ShipOrderCommand(ORDER_ID));

        assertEquals(OrderStatus.SHIPPING, order.getStatus());
        verify(orderRepository).save(order);
        verify(commitStock).execute(new CommitStockCommand(ORDER_ID));
        verify(events).publishEvent(any(OrderShippedEvent.class));
    }

    @Test
    void ship_orderThatIsNotConfirmedIsRejectedAndStockUntouched() {
        exists(codOrder(OrderStatus.PENDING));

        assertThrows(InvalidOperationException.class, () -> ship.execute(new ShipOrderCommand(ORDER_ID)));
        verifyNoInteractions(commitStock, events);
    }

    @Test
    void ship_alreadyShippingDoesNothing() {
        exists(codOrder(OrderStatus.SHIPPING));

        ship.execute(new ShipOrderCommand(ORDER_ID));

        verify(orderRepository, never()).save(any());
        verifyNoInteractions(commitStock, events);
    }

    // ---- deliver ----

    @Test
    void deliver_codOrderRecordsCashCollectedByTheCarrier() {
        Order order = codOrder(OrderStatus.SHIPPING);
        exists(order);

        deliver.execute(new DeliverOrderCommand(ORDER_ID));

        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        ArgumentCaptor<RecordPaymentCommand> payment = ArgumentCaptor.forClass(RecordPaymentCommand.class);
        verify(recordPayment).execute(payment.capture());
        assertEquals(CODE.value(), payment.getValue().orderReference());
        assertEquals(PaymentMethod.COD, payment.getValue().method());
        assertEquals(TOTAL.amount(), payment.getValue().amount());
        verify(events).publishEvent(any(OrderDeliveredEvent.class));
    }

    @Test
    void deliver_prepaidOrderRecordsNoPayment() {
        exists(prepaidPaid(OrderStatus.SHIPPING, "55"));

        deliver.execute(new DeliverOrderCommand(ORDER_ID));

        verifyNoInteractions(recordPayment);
        verify(events).publishEvent(any(OrderDeliveredEvent.class));
    }

    @Test
    void deliver_orderThatIsNotShippingIsRejected() {
        exists(codOrder(OrderStatus.CONFIRMED));

        assertThrows(InvalidOperationException.class, () -> deliver.execute(new DeliverOrderCommand(ORDER_ID)));
        verifyNoInteractions(recordPayment, events);
    }

    @Test
    void deliver_alreadyDeliveredDoesNotRecordPaymentTwice() {
        exists(codOrder(OrderStatus.DELIVERED));

        deliver.execute(new DeliverOrderCommand(ORDER_ID));

        verify(orderRepository, never()).save(any());
        verifyNoInteractions(recordPayment, events);
    }

    // ---- return ----

    @Test
    void return_restocksGoodsAndPublishesEvent() {
        Order order = codOrder(OrderStatus.SHIPPING);
        exists(order);

        returnOrder.execute(new ReturnOrderCommand(ORDER_ID));

        assertEquals(OrderStatus.RETURNED, order.getStatus());
        verify(orderRepository).save(order);
        verify(restockReturned).execute(new RestockReturnedCommand(ORDER_ID));
        verifyNoInteractions(requestRefund);
        verify(events).publishEvent(any(OrderReturnedEvent.class));
    }

    @Test
    void return_paidOrderAlsoRequestsRefund() {
        exists(prepaidPaid(OrderStatus.SHIPPING, "55"));

        returnOrder.execute(new ReturnOrderCommand(ORDER_ID));

        ArgumentCaptor<RequestRefundCommand> refund = ArgumentCaptor.forClass(RequestRefundCommand.class);
        verify(requestRefund).execute(refund.capture());
        assertEquals(55L, refund.getValue().paymentId());
    }

    @Test
    void return_orderThatIsNotShippingIsRejected() {
        exists(codOrder(OrderStatus.CONFIRMED));

        assertThrows(InvalidOperationException.class, () -> returnOrder.execute(new ReturnOrderCommand(ORDER_ID)));
        verifyNoInteractions(restockReturned, requestRefund, events);
    }

    @Test
    void return_alreadyReturnedDoesNothing() {
        exists(codOrder(OrderStatus.RETURNED));

        returnOrder.execute(new ReturnOrderCommand(ORDER_ID));

        verify(orderRepository, never()).save(any());
        verifyNoInteractions(restockReturned, requestRefund, events);
    }
}
