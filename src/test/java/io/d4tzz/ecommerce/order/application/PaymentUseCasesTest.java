package io.d4tzz.ecommerce.order.application;

import static io.d4tzz.ecommerce.order.domain.OrderFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.order.application.command.MarkOrderPaidCommand;
import io.d4tzz.ecommerce.order.application.command.MarkOrderRefundedCommand;
import io.d4tzz.ecommerce.order.application.command.PayMyOrderCommand;
import io.d4tzz.ecommerce.order.domain.MarkPaidResult;
import io.d4tzz.ecommerce.order.domain.MarkRefundedResult;
import io.d4tzz.ecommerce.order.domain.Order;
import io.d4tzz.ecommerce.order.domain.OrderRepository;
import io.d4tzz.ecommerce.order.domain.OrderStatus;
import io.d4tzz.ecommerce.order.domain.PaymentStatus;
import io.d4tzz.ecommerce.order.domain.event.OrderPaidEvent;
import io.d4tzz.ecommerce.order.domain.event.OrderRefundedEvent;
import io.d4tzz.ecommerce.payment.application.RequestPaymentUseCase;
import io.d4tzz.ecommerce.payment.application.RequestRefundUseCase;
import io.d4tzz.ecommerce.payment.application.command.RequestPaymentCommand;
import io.d4tzz.ecommerce.payment.application.command.RequestRefundCommand;
import io.d4tzz.ecommerce.payment.application.port.PaymentRedirect;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class PaymentUseCasesTest {

    private OrderRepository orderRepository;
    private ApplicationEventPublisher events;
    private RequestRefundUseCase requestRefund;
    private RequestPaymentUseCase requestPayment;

    private MarkOrderPaidUseCase markPaid;
    private MarkOrderRefundedUseCase markRefunded;
    private PayMyOrderUseCase payMyOrder;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        events = mock(ApplicationEventPublisher.class);
        requestRefund = mock(RequestRefundUseCase.class);
        requestPayment = mock(RequestPaymentUseCase.class);
        markPaid = new MarkOrderPaidUseCase(orderRepository, requestRefund, events);
        markRefunded = new MarkOrderRefundedUseCase(orderRepository, events);
        payMyOrder = new PayMyOrderUseCase(orderRepository, requestPayment);
    }

    private void locked(Order order) {
        when(orderRepository.findForUpdateByOrderCode(CODE)).thenReturn(Optional.of(order));
    }

    // ---- money received ----

    @Test
    void markPaid_acceptedPaymentIsSavedAndPublished() {
        Order order = prepaidUnpaidWithTimeLeft();
        locked(order);

        MarkPaidResult result = markPaid.execute(new MarkOrderPaidCommand(CODE.value(), 55L, TOTAL.amount()));

        assertEquals(MarkPaidResult.ACCEPTED, result);
        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
        assertEquals("55", order.getPaymentReference());
        verify(orderRepository).save(order);
        verify(events).publishEvent(any(OrderPaidEvent.class));
        verifyNoInteractions(requestRefund);
    }

    @Test
    void markPaid_paymentTheOrderCannotAcceptIsRefundedNotThrown() {
        Order order = prepaidUnpaid(OrderStatus.CANCELLED);
        locked(order);

        MarkPaidResult result = markPaid.execute(new MarkOrderPaidCommand(CODE.value(), 55L, TOTAL.amount()));

        assertEquals(MarkPaidResult.REJECTED_ORDER_CLOSED, result);
        ArgumentCaptor<RequestRefundCommand> refund = ArgumentCaptor.forClass(RequestRefundCommand.class);
        verify(requestRefund).execute(refund.capture());
        assertEquals(55L, refund.getValue().paymentId());
        assertTrue(refund.getValue().reason().contains("REJECTED_ORDER_CLOSED"));
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void markPaid_wrongAmountIsRefunded() {
        locked(prepaidUnpaidWithTimeLeft());

        MarkPaidResult result = markPaid.execute(new MarkOrderPaidCommand(CODE.value(), 55L, Money.of(1).amount()));

        assertEquals(MarkPaidResult.REJECTED_AMOUNT_MISMATCH, result);
        verify(requestRefund).execute(any(RequestRefundCommand.class));
    }

    @Test
    void markPaid_samePaymentAgainChangesNothingAndIsNotRefunded() {
        locked(prepaidPaid(OrderStatus.PENDING, "55"));

        MarkPaidResult result = markPaid.execute(new MarkOrderPaidCommand(CODE.value(), 55L, TOTAL.amount()));

        assertEquals(MarkPaidResult.DUPLICATE, result);
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(requestRefund, events);
    }

    @Test
    void markPaid_secondDifferentPaymentIsRefunded() {
        locked(prepaidPaid(OrderStatus.PENDING, "55"));

        MarkPaidResult result = markPaid.execute(new MarkOrderPaidCommand(CODE.value(), 56L, TOTAL.amount()));

        assertEquals(MarkPaidResult.REJECTED_ALREADY_PAID, result);
        ArgumentCaptor<RequestRefundCommand> refund = ArgumentCaptor.forClass(RequestRefundCommand.class);
        verify(requestRefund).execute(refund.capture());
        assertEquals(56L, refund.getValue().paymentId());
    }

    @Test
    void markPaid_unknownOrderIsReportedAndNotRefunded() {
        when(orderRepository.findForUpdateByOrderCode(CODE)).thenReturn(Optional.empty());

        MarkPaidResult result = markPaid.execute(new MarkOrderPaidCommand(CODE.value(), 55L, TOTAL.amount()));

        assertEquals(MarkPaidResult.ORDER_NOT_FOUND, result);
        verifyNoInteractions(requestRefund, events);
    }

    // ---- refund recorded ----

    @Test
    void markRefunded_cancelledPaidOrderBecomesRefunded() {
        Order order = prepaidPaid(OrderStatus.CANCELLED, "55");
        locked(order);

        MarkRefundedResult result = markRefunded.execute(new MarkOrderRefundedCommand(CODE.value(), 55L));

        assertEquals(MarkRefundedResult.REFUNDED, result);
        assertEquals(PaymentStatus.REFUNDED, order.getPaymentStatus());
        verify(orderRepository).save(order);
        verify(events).publishEvent(any(OrderRefundedEvent.class));
    }

    @Test
    void markRefunded_unexpectedRefundIsReportedNotThrown() {
        locked(prepaidPaid(OrderStatus.CONFIRMED, "55"));

        MarkRefundedResult result = markRefunded.execute(new MarkOrderRefundedCommand(CODE.value(), 55L));

        assertEquals(MarkRefundedResult.ORDER_STILL_ACTIVE, result);
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void markRefunded_refundOfAnotherPaymentLeavesOrderAlone() {
        locked(prepaidPaid(OrderStatus.CANCELLED, "55"));

        MarkRefundedResult result = markRefunded.execute(new MarkOrderRefundedCommand(CODE.value(), 99L));

        assertEquals(MarkRefundedResult.NOT_ORDER_PAYMENT, result);
        verifyNoInteractions(events);
    }

    @Test
    void markRefunded_unknownOrderIsReported() {
        when(orderRepository.findForUpdateByOrderCode(CODE)).thenReturn(Optional.empty());

        assertEquals(
            MarkRefundedResult.ORDER_NOT_FOUND,
            markRefunded.execute(new MarkOrderRefundedCommand(CODE.value(), 55L))
        );
    }

    // ---- customer pays ----

    private PayMyOrderCommand payCommand(long userId) {
        return new PayMyOrderCommand(userId, CODE.value(), "SEPAY", "1.2.3.4");
    }

    @Test
    void payMyOrder_returnsGatewayRedirectForTheOrderTotal() {
        Order order = prepaidUnpaidWithTimeLeft();
        when(orderRepository.findByOrderCode(CODE)).thenReturn(Optional.of(order));
        PaymentRedirect redirect = new PaymentRedirect("POST", "https://gateway", Map.of("a", "b"));
        when(requestPayment.execute(any())).thenReturn(redirect);

        PaymentRedirect result = payMyOrder.execute(payCommand(USER_ID));

        assertSame(redirect, result);
        ArgumentCaptor<RequestPaymentCommand> command = ArgumentCaptor.forClass(RequestPaymentCommand.class);
        verify(requestPayment).execute(command.capture());
        assertEquals(CODE.value(), command.getValue().orderCode());
        assertEquals(TOTAL.amount(), command.getValue().amount());
        assertEquals("SEPAY", command.getValue().gateway());
        assertEquals("1.2.3.4", command.getValue().clientIp());
        assertEquals(order.getPaymentExpiresAt(), command.getValue().expiresAt());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void payMyOrder_someoneElsesOrderLooksMissing() {
        when(orderRepository.findByOrderCode(CODE)).thenReturn(Optional.of(prepaidUnpaidWithTimeLeft()));

        assertThrows(NotFoundException.class, () -> payMyOrder.execute(payCommand(USER_ID + 1)));
        verifyNoInteractions(requestPayment);
    }

    @Test
    void payMyOrder_unknownCodeIsNotFound() {
        when(orderRepository.findByOrderCode(CODE)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> payMyOrder.execute(payCommand(USER_ID)));
    }

    @Test
    void payMyOrder_codOrderCannotBePaidOnline() {
        when(orderRepository.findByOrderCode(CODE)).thenReturn(Optional.of(codOrder(OrderStatus.PENDING)));

        assertThrows(InvalidOperationException.class, () -> payMyOrder.execute(payCommand(USER_ID)));
        verifyNoInteractions(requestPayment);
    }

    @Test
    void payMyOrder_alreadyPaidOrderIsRejected() {
        when(orderRepository.findByOrderCode(CODE)).thenReturn(Optional.of(prepaidPaid(OrderStatus.PENDING, "55")));

        assertThrows(InvalidOperationException.class, () -> payMyOrder.execute(payCommand(USER_ID)));
        verifyNoInteractions(requestPayment);
    }

    @Test
    void payMyOrder_afterDeadlineIsRejected() {
        // PAY_BY is in the past relative to the real clock
        when(orderRepository.findByOrderCode(CODE)).thenReturn(Optional.of(prepaidUnpaid(OrderStatus.PENDING)));

        assertThrows(InvalidOperationException.class, () -> payMyOrder.execute(payCommand(USER_ID)));
        verifyNoInteractions(requestPayment);
    }
}
