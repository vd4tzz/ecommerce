package io.d4tzz.ecommerce.domain.catalog.product;

import io.d4tzz.ecommerce.common.Identifier;

public class VariantId extends Identifier<Long> {

    public VariantId(Long id) {
        super(id);
    }

    public static VariantId of(Long id) {
        return new VariantId(id);
    }
}
