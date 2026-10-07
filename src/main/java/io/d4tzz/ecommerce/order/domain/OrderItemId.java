package io.d4tzz.ecommerce.order.domain;

import io.d4tzz.ecommerce.common.Identifier;

public class OrderItemId extends Identifier<Long> {

    public OrderItemId(Long id) {
        super(id);
    }

    public static OrderItemId of(Long id) {
        return new OrderItemId(id);
    }
}
