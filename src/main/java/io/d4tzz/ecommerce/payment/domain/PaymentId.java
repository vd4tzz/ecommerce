package io.d4tzz.ecommerce.payment.domain;

import io.d4tzz.ecommerce.common.Identifier;

public class PaymentId extends Identifier<Long> {

    public PaymentId(Long id) {
        super(id);
    }

    public static PaymentId of(Long id) {
        return new PaymentId(id);
    }
}
