package io.d4tzz.ecommerce.cart.domain;

import io.d4tzz.ecommerce.common.Identifier;

public class CartId extends Identifier<Long> {

    public CartId(Long id) {
        super(id);
    }

    public static CartId of(Long id) {
        return new CartId(id);
    }
}
