package io.d4tzz.ecommerce.payment.domain;

import io.d4tzz.ecommerce.common.Identifier;

public class RefundRequestId extends Identifier<Long> {

    public RefundRequestId(Long id) {
        super(id);
    }

    public static RefundRequestId of(Long id) {
        return new RefundRequestId(id);
    }
}
