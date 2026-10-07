package io.d4tzz.ecommerce.catalog.domain.product;

import io.d4tzz.ecommerce.common.NonBlankString;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import java.util.regex.Pattern;

public class ProductSlug extends NonBlankString {

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    public ProductSlug(String value) {
        super(value);
        if (!SLUG_PATTERN.matcher(value).matches()) {
            throw new InvalidArgumentException("Invalid slug format: " + value);
        }
    }

    public static ProductSlug of(String value) {
        return new ProductSlug(value);
    }
}
