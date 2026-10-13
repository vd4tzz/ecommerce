package io.d4tzz.ecommerce.inventory.domain;

import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import java.util.regex.Pattern;

/**
 * The short unique name of a warehouse, e.g. {@code HN-01}. Trimmed and upper-cased; 2-50 letters, digits, '_' or
 * '-', otherwise an {@link InvalidArgumentException} is thrown.
 */
public record WarehouseCode(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[A-Z0-9_-]{2,50}$");

    public WarehouseCode {
        value = value.trim().toUpperCase();
        if (!PATTERN.matcher(value).matches()) {
            throw new InvalidArgumentException(
                "Warehouse code must be 2-50 characters of letters, digits, '_' or '-'"
            );
        }
    }

    public static WarehouseCode of(String value) {
        return new WarehouseCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
