package io.d4tzz.ecommerce.order.domain;

import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.domain.user.Address;
import io.d4tzz.ecommerce.domain.user.UserId;
import io.d4tzz.ecommerce.order.domain.event.OrderLine;
import java.time.Instant;
import java.util.List;

/**
 * Order aggregate.
 *
 * <p>Order status (fulfillment) and payment status are tracked separately:
 * a COD order is shipped while still UNPAID, a prepaid order must be PAID before it can be confirmed.
 *
 * <p>State-changing methods return {@code true} when the state actually changed and {@code false} when
 * the call was a no-op (already in the target state), so the application layer only publishes events
 * for real changes.
 */
public class Order {

    public static final String PAYMENT_EXPIRED_REASON = "Payment expired";

    private final OrderId id;
    private final OrderCode orderCode;
    private final UserId userId;
    private final Recipient recipient;
    private final Address shippingAddress;
    private final List<OrderItem> items;
    private final Money subtotal;
    private final Money shippingFee;
    private final Money totalAmount;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private final String note;
    // null for cash-on-delivery orders
    private final Instant paymentExpiresAt;
    private Instant paidAt;
    // identifies the accepted payment, so a redelivered payment can be told apart from a second one
    private String paymentReference;
    private String cancelReason;
    private Instant cancelledAt;
    private Instant completedAt;
    private final Instant createdAt;
    private Instant updatedAt;

    private Order(
        OrderId id,
        OrderCode orderCode,
        UserId userId,
        Recipient recipient,
        Address shippingAddress,
        List<OrderItem> items,
        Money shippingFee,
        Money totalAmount,
        OrderStatus status,
        PaymentStatus paymentStatus,
        String note,
        Instant paymentExpiresAt,
        Instant paidAt,
        String paymentReference,
        String cancelReason,
        Instant cancelledAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt
    ) {
        if (items.isEmpty()) {
            throw new InvalidArgumentException("Order must contain at least one item");
        }

        Money calculatedSubtotal = items.stream().map(OrderItem::getTotalPrice).reduce(Money.ZERO, Money::plus);
        Money calculatedTotal = calculatedSubtotal.plus(shippingFee);
        if (totalAmount != null && !totalAmount.equals(calculatedTotal)) {
            throw new InvalidArgumentException(
                "Total amount " + totalAmount + " does not match items and shipping fee (" + calculatedTotal + ")"
            );
        }

        this.id = id;
        this.orderCode = orderCode;
        this.userId = userId;
        this.recipient = recipient;
        this.shippingAddress = shippingAddress;
        this.items = List.copyOf(items);
        this.subtotal = calculatedSubtotal;
        this.shippingFee = shippingFee;
        this.totalAmount = calculatedTotal;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.note = note != null ? note.trim() : "";
        this.paymentExpiresAt = paymentExpiresAt;
        this.paidAt = paidAt;
        this.paymentReference = paymentReference;
        this.cancelReason = cancelReason;
        this.cancelledAt = cancelledAt;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Order createPrepaid(
        OrderCode orderCode,
        UserId userId,
        Recipient recipient,
        Address shippingAddress,
        List<OrderItem> items,
        Money shippingFee,
        String note,
        Instant paymentExpiresAt,
        Instant now
    ) {
        if (!paymentExpiresAt.isAfter(now)) {
            throw new InvalidArgumentException("Payment expiry must be in the future");
        }
        return create(orderCode, userId, recipient, shippingAddress, items, shippingFee, note, paymentExpiresAt, now);
    }

    public static Order createCashOnDelivery(
        OrderCode orderCode,
        UserId userId,
        Recipient recipient,
        Address shippingAddress,
        List<OrderItem> items,
        Money shippingFee,
        String note,
        Instant now
    ) {
        return create(orderCode, userId, recipient, shippingAddress, items, shippingFee, note, null, now);
    }

    private static Order create(
        OrderCode orderCode,
        UserId userId,
        Recipient recipient,
        Address shippingAddress,
        List<OrderItem> items,
        Money shippingFee,
        String note,
        Instant paymentExpiresAt,
        Instant now
    ) {
        Order order = new Order(
            null,
            orderCode,
            userId,
            recipient,
            shippingAddress,
            items,
            shippingFee,
            null,
            OrderStatus.PENDING,
            PaymentStatus.UNPAID,
            note,
            paymentExpiresAt,
            null,
            null,
            null,
            null,
            null,
            now,
            now
        );
        // Nothing to collect: the order is paid from the start, without any payment record
        if (order.totalAmount.isZero()) {
            order.paymentStatus = PaymentStatus.PAID;
            order.paidAt = now;
        }
        return order;
    }

    public static Order restore(
        OrderId id,
        OrderCode orderCode,
        UserId userId,
        Recipient recipient,
        Address shippingAddress,
        List<OrderItem> items,
        Money shippingFee,
        Money totalAmount,
        OrderStatus status,
        PaymentStatus paymentStatus,
        String note,
        Instant paymentExpiresAt,
        Instant paidAt,
        String paymentReference,
        String cancelReason,
        Instant cancelledAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt
    ) {
        return new Order(
            id,
            orderCode,
            userId,
            recipient,
            shippingAddress,
            items,
            shippingFee,
            totalAmount,
            status,
            paymentStatus,
            note,
            paymentExpiresAt,
            paidAt,
            paymentReference,
            cancelReason,
            cancelledAt,
            completedAt,
            createdAt,
            updatedAt
        );
    }

    // ---- Fulfillment ----

    public boolean confirm(Instant now) {
        if (status == OrderStatus.CONFIRMED) {
            return false;
        }
        if (requiresPrepayment() && paymentStatus != PaymentStatus.PAID) {
            throw new InvalidOperationException("Prepaid order must be paid before confirmation");
        }
        transitionTo(OrderStatus.CONFIRMED, now);
        return true;
    }

    public boolean markShipping(Instant now) {
        if (status == OrderStatus.SHIPPING) {
            return false;
        }
        transitionTo(OrderStatus.SHIPPING, now);
        return true;
    }

    public boolean markDelivered(Instant now) {
        if (status == OrderStatus.DELIVERED) {
            return false;
        }
        transitionTo(OrderStatus.DELIVERED, now);
        this.completedAt = now;
        return true;
    }

    public boolean markReturned(Instant now) {
        if (status == OrderStatus.RETURNED) {
            return false;
        }
        transitionTo(OrderStatus.RETURNED, now);
        this.completedAt = now;
        return true;
    }

    public boolean cancel(String reason, Instant now) {
        if (status == OrderStatus.CANCELLED) {
            return false;
        }
        if (reason.isBlank()) {
            throw new InvalidArgumentException("Cancel reason must not be blank");
        }
        transitionTo(OrderStatus.CANCELLED, now);
        this.cancelReason = reason.trim();
        this.cancelledAt = now;
        return true;
    }

    /** Customers may only cancel before the shop confirms; after that only an admin can, via {@link #cancel}. */
    public boolean cancelByCustomer(String reason, Instant now) {
        if (status != OrderStatus.PENDING && status != OrderStatus.CANCELLED) {
            throw new InvalidOperationException("Order can no longer be cancelled by the customer, status: " + status);
        }
        return cancel(reason, now);
    }

    public boolean expire(Instant now) {
        if (!isPaymentExpired(now)) {
            return false;
        }
        return cancel(PAYMENT_EXPIRED_REASON, now);
    }

    // ---- Payment ----

    /**
     * Applies money received for this order. Never throws for a payment that cannot be accepted: the money has
     * already moved, so the caller needs the reason to refund it rather than an error.
     */
    public MarkPaidResult markPaid(String paymentReference, Money amount, Instant now) {
        if (paymentReference.isBlank()) {
            throw new InvalidArgumentException("Payment reference must not be blank");
        }
        if (paymentStatus == PaymentStatus.PAID) {
            return paymentReference.equals(this.paymentReference)
                ? MarkPaidResult.DUPLICATE
                : MarkPaidResult.REJECTED_ALREADY_PAID;
        }
        if (paymentStatus == PaymentStatus.REFUNDED) {
            return MarkPaidResult.REJECTED_REFUNDED;
        }
        if (status == OrderStatus.CANCELLED || status == OrderStatus.RETURNED) {
            return MarkPaidResult.REJECTED_ORDER_CLOSED;
        }
        if (isPaymentExpired(now)) {
            // Deadline passed but the expiry job has not cancelled the order yet
            return MarkPaidResult.REJECTED_EXPIRED;
        }
        if (!amount.equals(totalAmount)) {
            return MarkPaidResult.REJECTED_AMOUNT_MISMATCH;
        }
        this.paymentStatus = PaymentStatus.PAID;
        this.paidAt = now;
        this.paymentReference = paymentReference;
        this.updatedAt = now;
        return MarkPaidResult.ACCEPTED;
    }

    /**
     * Applies a full refund recorded by the payment side. Like {@link #markPaid}, it reports instead of throwing,
     * because the money has already gone back.
     *
     * @param paymentReference the refunded payment; only the payment this order accepted affects the order
     */
    public MarkRefundedResult markRefunded(String paymentReference, Instant now) {
        if (!paymentReference.equals(this.paymentReference)) {
            // e.g. the refund of a duplicate payment the order rejected
            return MarkRefundedResult.NOT_ORDER_PAYMENT;
        }
        if (paymentStatus == PaymentStatus.REFUNDED) {
            return MarkRefundedResult.DUPLICATE;
        }
        if (!qualifiesForRefund()) {
            return MarkRefundedResult.ORDER_STILL_ACTIVE;
        }
        this.paymentStatus = PaymentStatus.REFUNDED;
        this.updatedAt = now;
        return MarkRefundedResult.REFUNDED;
    }

    // ---- Queries ----

    public boolean requiresPrepayment() {
        return paymentExpiresAt != null;
    }

    public boolean isPaymentExpired(Instant now) {
        return (
            status == OrderStatus.PENDING &&
            paymentStatus == PaymentStatus.UNPAID &&
            requiresPrepayment() &&
            now.isAfter(paymentExpiresAt)
        );
    }

    /** Throws unless the customer can still pay for this order online. */
    public void ensureAwaitingPayment(Instant now) {
        if (!requiresPrepayment()) {
            throw new InvalidOperationException("Order " + orderCode + " is paid on delivery");
        }
        if (paymentStatus != PaymentStatus.UNPAID) {
            throw new InvalidOperationException("Order " + orderCode + " is already " + paymentStatus);
        }
        if (status != OrderStatus.PENDING) {
            throw new InvalidOperationException("Order " + orderCode + " is " + status);
        }
        if (isPaymentExpired(now)) {
            throw new InvalidOperationException("The time to pay for order " + orderCode + " is over");
        }
    }

    public boolean canCancel() {
        return status.canTransitionTo(OrderStatus.CANCELLED);
    }

    /** The customer paid, but the order was cancelled or returned, so the money should go back. */
    public boolean qualifiesForRefund() {
        // A free order is PAID without a payment, so there is nothing to give back
        return (
            paymentStatus == PaymentStatus.PAID &&
            paymentReference != null &&
            (status == OrderStatus.CANCELLED || status == OrderStatus.RETURNED)
        );
    }

    public List<OrderLine> lines() {
        return items.stream().map(OrderItem::toLine).toList();
    }

    public int getTotalQuantity() {
        return items.stream().mapToInt(OrderItem::getQuantity).sum();
    }

    private void transitionTo(OrderStatus next, Instant now) {
        if (!status.canTransitionTo(next)) {
            throw new InvalidOperationException("Cannot change order status from " + status + " to " + next);
        }
        this.status = next;
        this.updatedAt = now;
    }

    // ---- Getters ----

    public OrderId getId() {
        return id;
    }

    public OrderCode getOrderCode() {
        return orderCode;
    }

    public UserId getUserId() {
        return userId;
    }

    public Recipient getRecipient() {
        return recipient;
    }

    public Address getShippingAddress() {
        return shippingAddress;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public Money getSubtotal() {
        return subtotal;
    }

    public Money getShippingFee() {
        return shippingFee;
    }

    public Money getTotalAmount() {
        return totalAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public String getNote() {
        return note;
    }

    public Instant getPaymentExpiresAt() {
        return paymentExpiresAt;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
