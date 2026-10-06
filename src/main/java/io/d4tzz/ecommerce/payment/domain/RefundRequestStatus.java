package io.d4tzz.ecommerce.payment.domain;

public enum RefundRequestStatus {
    /** The money still has to be sent back. */
    PENDING,
    /** The money was sent back and the refund is recorded. */
    DONE,
}
