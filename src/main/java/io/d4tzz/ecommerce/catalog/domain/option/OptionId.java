package io.d4tzz.ecommerce.catalog.domain.option;

import io.d4tzz.ecommerce.common.Identifier;

public class OptionId extends Identifier<Long> {

    public OptionId(Long id) {
        super(id);
    }

    public static OptionId of(Long id) {
        return new OptionId(id);
    }
}
