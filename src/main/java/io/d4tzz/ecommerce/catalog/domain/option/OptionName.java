package io.d4tzz.ecommerce.catalog.domain.option;

import io.d4tzz.ecommerce.common.NonBlankString;

public class OptionName extends NonBlankString {

    public OptionName(String value) {
        super(value);
    }

    public static OptionName of(String value) {
        return new OptionName(value);
    }
}
