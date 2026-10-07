package io.d4tzz.ecommerce.cart.domain;

import io.d4tzz.ecommerce.common.Identifier;

public class CartItemId extends Identifier<Long> {

    public CartItemId(Long id) {
        super(id);
    }

    public static CartItemId of(Long id) {
        return new CartItemId(id);
    }
}
