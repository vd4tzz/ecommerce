package io.d4tzz.ecommerce.order.application.port;

/** Lets order hand money it will not accept back to the payment side. */
public interface PaymentRefunds {
    void requestRefund(Long paymentId, String reason);
}
