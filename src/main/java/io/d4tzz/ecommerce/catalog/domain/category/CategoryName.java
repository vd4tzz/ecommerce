package io.d4tzz.ecommerce.catalog.domain.category;

import io.d4tzz.ecommerce.common.NonBlankString;

public class CategoryName extends NonBlankString {

    public CategoryName(String value) {
        super(value);
    }

    public static CategoryName of(String value) {
        return new CategoryName(value);
    }
}
