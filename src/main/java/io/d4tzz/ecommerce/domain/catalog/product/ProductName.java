package io.d4tzz.ecommerce.domain.catalog.product;

import io.d4tzz.ecommerce.common.NonBlankString;

public class ProductName extends NonBlankString {

    public ProductName(String value) {
        super(value);
    }

    public static ProductName of(String value) {
        return new ProductName(value);
    }
}
