package io.d4tzz.ecommerce.order.domain;

import io.d4tzz.ecommerce.common.Identifier;

public class OrderId extends Identifier<Long> {

    public OrderId(Long id) {
        super(id);
    }

    public static OrderId of(Long id) {
        return new OrderId(id);
    }
}
