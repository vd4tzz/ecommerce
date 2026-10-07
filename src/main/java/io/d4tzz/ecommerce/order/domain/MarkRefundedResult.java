package io.d4tzz.ecommerce.order.domain;

/** Outcome of {@link Order#markRefunded}. */
public enum MarkRefundedResult {
    REFUNDED,
    /** The same refund delivered again: nothing to do. */
    DUPLICATE,
    /** The refunded payment is not the one this order accepted: the order is not affected. */
    NOT_ORDER_PAYMENT,
    /**
     * The order's own payment was refunded while the order is neither cancelled nor returned. The order cannot
     * express that, so a person has to look at it.
     */
    ORDER_STILL_ACTIVE,
    /** No order with the refund's order id: set by the use case, never by {@link Order#markRefunded}. */
    ORDER_NOT_FOUND,
}
