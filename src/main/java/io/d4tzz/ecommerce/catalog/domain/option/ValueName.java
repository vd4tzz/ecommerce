package io.d4tzz.ecommerce.catalog.domain.option;

import io.d4tzz.ecommerce.common.NonBlankString;

public class ValueName extends NonBlankString {

    public ValueName(String value) {
        super(value);
    }

    public static ValueName of(String value) {
        return new ValueName(value);
    }
}
