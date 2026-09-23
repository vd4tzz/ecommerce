package io.d4tzz.ecommerce.domain.user;

import io.d4tzz.ecommerce.common.NonBlankString;

public class FullName extends NonBlankString {

    public FullName(String value) {
        super(value);
    }

    public static FullName of(String value) {
        return new FullName(value);
    }
}
