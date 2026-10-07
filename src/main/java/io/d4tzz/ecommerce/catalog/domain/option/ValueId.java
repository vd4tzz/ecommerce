package io.d4tzz.ecommerce.catalog.domain.option;

import io.d4tzz.ecommerce.common.Identifier;

public class ValueId extends Identifier<Long> {

    public ValueId(Long id) {
        super(id);
    }

    public static ValueId of(Long id) {
        return new ValueId(id);
    }
}
