package io.d4tzz.ecommerce.payment.domain;

public enum PaymentType {
    /** Money received from the customer. */
    PAYMENT,
    /** Money sent back to the customer, always against one PAYMENT. */
    REFUND,
}
