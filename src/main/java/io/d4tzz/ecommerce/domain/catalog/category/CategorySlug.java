package io.d4tzz.ecommerce.domain.catalog.category;

import java.util.regex.Pattern;

import io.d4tzz.ecommerce.common.NonBlankString;

public class CategorySlug extends NonBlankString {

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    public CategorySlug(String value) {
        super(value);
        if (!SLUG_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Invalid category slug format: " + value);
        }
    }

    public static CategorySlug of(String value) {
        return new CategorySlug(value);
    }

}
