package io.d4tzz.ecommerce.order.domain;

/** Result of {@link Order#markPaid}. For every REJECTED_* result, the shop must give the money back. */
public enum MarkPaidResult {
    /** The order is now paid. */
    ACCEPTED,
    /** Got the same payment again. The order is already paid by it, so nothing changes. */
    DUPLICATE,
    /** The order is already paid, and this is a second, different payment (the customer paid twice). */
    REJECTED_ALREADY_PAID,
    /** The order's money was already given back to the customer. */
    REJECTED_REFUNDED,
    /** The order was cancelled or returned before the money came. */
    REJECTED_ORDER_CLOSED,
    /** The time to pay is over, even if the order has not been cancelled yet. */
    REJECTED_EXPIRED,
    /** The amount paid is not the order total. */
    REJECTED_AMOUNT_MISMATCH,
    /**
     * There is no order with this code (e.g. the customer mistyped it). Not a rejection: the money stays with
     * payment, unmatched, until a person assigns it to the right order. Only the use case returns this, not
     * {@link Order#markPaid}.
     */
    ORDER_NOT_FOUND;

    public boolean isRejected() {
        return name().startsWith("REJECTED_");
    }
}
