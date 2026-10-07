package io.d4tzz.ecommerce.order.application.port;

import io.d4tzz.ecommerce.common.Money;

/** Lets order tell the payment side that the carrier collected the cash of a COD order. */
public interface CashOnDeliveryPayments {
    /** Records the cash as a payment for the order; the order then becomes paid like for any other payment. */
    void recordCollected(String orderCode, Money amount);
}
