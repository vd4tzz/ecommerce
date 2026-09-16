package io.d4tzz.ecommerce.domain.catalog.product;

import io.d4tzz.ecommerce.common.Identifier;

public class ProductId extends Identifier<Long> {

    public ProductId(Long id) {
        super(id);
    }

    public static ProductId of(Long id) {
        return new ProductId(id);
    }
}
