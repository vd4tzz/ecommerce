package io.d4tzz.ecommerce.order.domain;

import static io.d4tzz.ecommerce.order.domain.OrderFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.user.domain.UserId;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderTest {

    private static final Instant AFTER_DEADLINE = PAY_BY.plusSeconds(1);

    // ---- creation ----

    @Test
    void createPrepaid_startsPendingAndUnpaidWithTotalFromItemsAndShipping() {
        Order order = newPrepaid();

        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(PaymentStatus.UNPAID, order.getPaymentStatus());
        assertTrue(order.requiresPrepayment());
        assertEquals(SUBTOTAL, order.getSubtotal());
        assertEquals(TOTAL, order.getTotalAmount());
        assertEquals(2, order.getTotalQuantity());
    }

    @Test
    void createPrepaid_rejectsDeadlineNotInTheFuture() {
        assertThrows(InvalidArgumentException.class, () ->
            Order.createPrepaid(
                CODE, UserId.of(USER_ID), recipient(), address(), List.of(item(100_000, 1)), SHIPPING_FEE, null, NOW, NOW
            )
        );
    }

    @Test
    void createCashOnDelivery_hasNoPaymentDeadline() {
        Order order = newCod();

        assertFalse(order.requiresPrepayment());
        assertNull(order.getPaymentExpiresAt());
        assertEquals(PaymentStatus.UNPAID, order.getPaymentStatus());
    }

    @Test
    void create_freeOrderIsPaidFromTheStartWithoutPaymentReference() {
        Order order = Order.createCashOnDelivery(
            CODE, UserId.of(USER_ID), recipient(), address(), List.of(item(0, 1)), Money.ZERO, null, NOW
        );

        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
        assertEquals(NOW, order.getPaidAt());
        assertNull(order.getPaymentReference());
    }

    @Test
    void create_rejectsOrderWithoutItems() {
        assertThrows(InvalidArgumentException.class, () ->
            Order.createCashOnDelivery(CODE, UserId.of(USER_ID), recipient(), address(), List.of(), SHIPPING_FEE, null, NOW)
        );
    }

    @Test
    void restore_rejectsTotalThatDoesNotMatchItemsAndShipping() {
        assertThrows(InvalidArgumentException.class, () ->
            Order.restore(
                OrderId.of(1L), CODE, UserId.of(USER_ID), recipient(), address(), List.of(item(100_000, 2)),
                SHIPPING_FEE, Money.of(1), OrderStatus.PENDING, PaymentStatus.UNPAID, null, null, null, null, null,
                null, null, NOW, NOW
            )
        );
    }

    @Test
    void create_trimsNoteAndDefaultsToEmpty() {
        assertEquals("", newCod().getNote());
        Order withNote = Order.createCashOnDelivery(
            CODE, UserId.of(USER_ID), recipient(), address(), List.of(item(100_000, 1)), SHIPPING_FEE, "  call first ", NOW
        );
        assertEquals("call first", withNote.getNote());
    }

    // ---- fulfillment ----

    @Test
    void confirm_prepaidOrderMustBePaidFirst() {
        Order order = prepaidUnpaid(OrderStatus.PENDING);

        assertThrows(InvalidOperationException.class, () -> order.confirm(NOW));
        assertEquals(OrderStatus.PENDING, order.getStatus());
    }

    @Test
    void confirm_paidPrepaidOrderIsConfirmed() {
        Order order = prepaidPaid(OrderStatus.PENDING, "55");

        assertTrue(order.confirm(NOW));
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }

    @Test
    void confirm_codOrderNeedsNoPayment() {
        Order order = codOrder(OrderStatus.PENDING);

        assertTrue(order.confirm(NOW));
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }

    @Test
    void confirm_alreadyConfirmedIsNoOp() {
        assertFalse(codOrder(OrderStatus.CONFIRMED).confirm(NOW));
    }

    @Test
    void confirm_rejectsOrderThatIsNotPending() {
        assertThrows(InvalidOperationException.class, () -> codOrder(OrderStatus.SHIPPING).confirm(NOW));
    }

    @Test
    void markShipping_requiresConfirmed() {
        assertThrows(InvalidOperationException.class, () -> codOrder(OrderStatus.PENDING).markShipping(NOW));

        Order order = codOrder(OrderStatus.CONFIRMED);
        assertTrue(order.markShipping(NOW));
        assertEquals(OrderStatus.SHIPPING, order.getStatus());
        assertFalse(order.markShipping(NOW));
    }

    @Test
    void markDelivered_requiresShippingAndSetsCompletedAt() {
        assertThrows(InvalidOperationException.class, () -> codOrder(OrderStatus.CONFIRMED).markDelivered(NOW));

        Order order = codOrder(OrderStatus.SHIPPING);
        assertTrue(order.markDelivered(NOW));
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        assertEquals(NOW, order.getCompletedAt());
        assertFalse(order.markDelivered(NOW));
    }

    @Test
    void markReturned_requiresShippingAndSetsCompletedAt() {
        assertThrows(InvalidOperationException.class, () -> codOrder(OrderStatus.CONFIRMED).markReturned(NOW));

        Order order = codOrder(OrderStatus.SHIPPING);
        assertTrue(order.markReturned(NOW));
        assertEquals(OrderStatus.RETURNED, order.getStatus());
        assertEquals(NOW, order.getCompletedAt());
        assertFalse(order.markReturned(NOW));
    }

    // ---- cancellation ----

    @Test
    void cancel_recordsTrimmedReasonAndTime() {
        Order order = codOrder(OrderStatus.CONFIRMED);

        assertTrue(order.cancel("  out of stock ", NOW));
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals("out of stock", order.getCancelReason());
        assertEquals(NOW, order.getCancelledAt());
    }

    @Test
    void cancel_rejectsBlankReason() {
        assertThrows(InvalidArgumentException.class, () -> codOrder(OrderStatus.PENDING).cancel("  ", NOW));
    }

    @Test
    void cancel_rejectsOrderAlreadyHandedToCarrier() {
        assertThrows(InvalidOperationException.class, () -> codOrder(OrderStatus.SHIPPING).cancel("x", NOW));
        assertThrows(InvalidOperationException.class, () -> codOrder(OrderStatus.DELIVERED).cancel("x", NOW));
    }

    @Test
    void cancel_alreadyCancelledIsNoOp() {
        assertFalse(codOrder(OrderStatus.CANCELLED).cancel("again", NOW));
    }

    @Test
    void cancelByCustomer_onlyWhilePending() {
        assertTrue(codOrder(OrderStatus.PENDING).cancelByCustomer("changed my mind", NOW));
        assertFalse(codOrder(OrderStatus.CANCELLED).cancelByCustomer("again", NOW));
        assertThrows(InvalidOperationException.class, () ->
            codOrder(OrderStatus.CONFIRMED).cancelByCustomer("changed my mind", NOW)
        );
    }

    @Test
    void canCancel_followsStatusTransitions() {
        assertTrue(codOrder(OrderStatus.PENDING).canCancel());
        assertTrue(codOrder(OrderStatus.CONFIRMED).canCancel());
        assertFalse(codOrder(OrderStatus.SHIPPING).canCancel());
        assertFalse(codOrder(OrderStatus.DELIVERED).canCancel());
    }

    // ---- expiry ----

    @Test
    void expire_cancelsUnpaidPrepaidOrderAfterDeadline() {
        Order order = prepaidUnpaid(OrderStatus.PENDING);

        assertTrue(order.expire(AFTER_DEADLINE));
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(Order.PAYMENT_EXPIRED_REASON, order.getCancelReason());
    }

    @Test
    void expire_doesNothingBeforeDeadlineForPaidOrCodOrders() {
        assertFalse(prepaidUnpaid(OrderStatus.PENDING).expire(PAY_BY));
        assertFalse(prepaidPaid(OrderStatus.PENDING, "55").expire(AFTER_DEADLINE));
        assertFalse(codOrder(OrderStatus.PENDING).expire(AFTER_DEADLINE));
    }

    // ---- payment ----

    @Test
    void markPaid_acceptsExactAmountBeforeDeadline() {
        Order order = prepaidUnpaid(OrderStatus.PENDING);

        assertEquals(MarkPaidResult.ACCEPTED, order.markPaid("55", TOTAL, NOW));
        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
        assertEquals("55", order.getPaymentReference());
        assertEquals(NOW, order.getPaidAt());
    }

    @Test
    void markPaid_samePaymentAgainIsDuplicate() {
        Order order = prepaidPaid(OrderStatus.PENDING, "55");

        assertEquals(MarkPaidResult.DUPLICATE, order.markPaid("55", TOTAL, NOW));
    }

    @Test
    void markPaid_secondDifferentPaymentIsRejected() {
        Order order = prepaidPaid(OrderStatus.PENDING, "55");

        assertEquals(MarkPaidResult.REJECTED_ALREADY_PAID, order.markPaid("56", TOTAL, NOW));
        assertEquals("55", order.getPaymentReference());
    }

    @Test
    void markPaid_refundedOrderRejectsPayment() {
        Order order = restored(OrderStatus.CANCELLED, PaymentStatus.REFUNDED, PAY_BY, "55");

        assertEquals(MarkPaidResult.REJECTED_REFUNDED, order.markPaid("56", TOTAL, NOW));
    }

    @Test
    void markPaid_cancelledOrReturnedOrderRejectsPayment() {
        assertEquals(
            MarkPaidResult.REJECTED_ORDER_CLOSED,
            prepaidUnpaid(OrderStatus.CANCELLED).markPaid("55", TOTAL, NOW)
        );
        assertEquals(
            MarkPaidResult.REJECTED_ORDER_CLOSED,
            prepaidUnpaid(OrderStatus.RETURNED).markPaid("55", TOTAL, NOW)
        );
    }

    @Test
    void markPaid_afterDeadlineIsRejectedEvenIfJobHasNotCancelledYet() {
        Order order = prepaidUnpaid(OrderStatus.PENDING);

        assertEquals(MarkPaidResult.REJECTED_EXPIRED, order.markPaid("55", TOTAL, AFTER_DEADLINE));
        assertEquals(PaymentStatus.UNPAID, order.getPaymentStatus());
    }

    @Test
    void markPaid_wrongAmountIsRejected() {
        Order order = prepaidUnpaid(OrderStatus.PENDING);

        assertEquals(MarkPaidResult.REJECTED_AMOUNT_MISMATCH, order.markPaid("55", Money.of(1), NOW));
        assertEquals(PaymentStatus.UNPAID, order.getPaymentStatus());
    }

    @Test
    void markPaid_rejectsBlankReferenceWithAnError() {
        assertThrows(InvalidArgumentException.class, () -> prepaidUnpaid(OrderStatus.PENDING).markPaid(" ", TOTAL, NOW));
    }

    @Test
    void markPaid_codOrderCanBePaidOnDelivery() {
        Order order = codOrder(OrderStatus.DELIVERED);

        assertEquals(MarkPaidResult.ACCEPTED, order.markPaid("55", TOTAL, NOW));
    }

    // ---- refund ----

    @Test
    void markRefunded_cancelledPaidOrderBecomesRefunded() {
        Order order = prepaidPaid(OrderStatus.CANCELLED, "55");

        assertEquals(MarkRefundedResult.REFUNDED, order.markRefunded("55", NOW));
        assertEquals(PaymentStatus.REFUNDED, order.getPaymentStatus());
    }

    @Test
    void markRefunded_returnedPaidOrderBecomesRefunded() {
        assertEquals(MarkRefundedResult.REFUNDED, prepaidPaid(OrderStatus.RETURNED, "55").markRefunded("55", NOW));
    }

    @Test
    void markRefunded_otherPaymentDoesNotAffectOrder() {
        Order order = prepaidPaid(OrderStatus.CANCELLED, "55");

        assertEquals(MarkRefundedResult.NOT_ORDER_PAYMENT, order.markRefunded("99", NOW));
        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
    }

    @Test
    void markRefunded_sameRefundAgainIsDuplicate() {
        Order order = restored(OrderStatus.CANCELLED, PaymentStatus.REFUNDED, PAY_BY, "55");

        assertEquals(MarkRefundedResult.DUPLICATE, order.markRefunded("55", NOW));
    }

    @Test
    void markRefunded_activeOrderNeedsManualCheck() {
        Order order = prepaidPaid(OrderStatus.CONFIRMED, "55");

        assertEquals(MarkRefundedResult.ORDER_STILL_ACTIVE, order.markRefunded("55", NOW));
        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
    }

    @Test
    void qualifiesForRefund_onlyWhenPaidWithAPaymentAndOrderIsClosed() {
        assertTrue(prepaidPaid(OrderStatus.CANCELLED, "55").qualifiesForRefund());
        assertTrue(prepaidPaid(OrderStatus.RETURNED, "55").qualifiesForRefund());
        assertFalse(prepaidPaid(OrderStatus.CONFIRMED, "55").qualifiesForRefund());
        assertFalse(prepaidUnpaid(OrderStatus.CANCELLED).qualifiesForRefund());
    }

    @Test
    void qualifiesForRefund_freeOrderHasNothingToGiveBack() {
        Order order = Order.createCashOnDelivery(
            CODE, UserId.of(USER_ID), recipient(), address(), List.of(item(0, 1)), Money.ZERO, null, NOW
        );
        order.cancel("changed my mind", NOW);

        assertFalse(order.qualifiesForRefund());
    }

    // ---- online payment ----

    @Test
    void ensureAwaitingPayment_acceptsUnpaidPendingPrepaidOrderBeforeDeadline() {
        assertDoesNotThrow(() -> prepaidUnpaid(OrderStatus.PENDING).ensureAwaitingPayment(NOW));
    }

    @Test
    void ensureAwaitingPayment_rejectsCodAlreadyPaidNotPendingAndExpired() {
        assertThrows(InvalidOperationException.class, () -> codOrder(OrderStatus.PENDING).ensureAwaitingPayment(NOW));
        assertThrows(InvalidOperationException.class, () ->
            prepaidPaid(OrderStatus.PENDING, "55").ensureAwaitingPayment(NOW)
        );
        assertThrows(InvalidOperationException.class, () ->
            prepaidUnpaid(OrderStatus.CANCELLED).ensureAwaitingPayment(NOW)
        );
        assertThrows(InvalidOperationException.class, () ->
            prepaidUnpaid(OrderStatus.PENDING).ensureAwaitingPayment(AFTER_DEADLINE)
        );
    }

    @Test
    void isPaymentExpired_isComputedFromDeadlineEvenBeforeJobRuns() {
        Order order = prepaidUnpaid(OrderStatus.PENDING);

        assertFalse(order.isPaymentExpired(PAY_BY));
        assertTrue(order.isPaymentExpired(AFTER_DEADLINE));
    }

    @Test
    void lines_listsVariantAndQuantityOfEveryItem() {
        var lines = newCod().lines();

        assertEquals(1, lines.size());
        assertEquals(2, lines.get(0).quantity());
    }
}
