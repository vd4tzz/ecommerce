package io.d4tzz.ecommerce.catalog.domain.product;

import io.d4tzz.ecommerce.common.NonBlankString;

public class Sku extends NonBlankString {

    public Sku(String value) {
        super(value);
    }

    public static Sku of(String value) {
        return new Sku(value);
    }
}
