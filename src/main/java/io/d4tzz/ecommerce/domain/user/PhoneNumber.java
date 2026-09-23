package io.d4tzz.ecommerce.domain.user;

import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import java.util.Objects;
import java.util.regex.Pattern;

public class PhoneNumber {

    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^(\\+?[0-9]{1,3})?[0-9]{9,11}$"
    );

    private final String value;

    public PhoneNumber(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidArgumentException("Phone number must not be blank");
        }
        String clean = value.replaceAll("[\\s.-]", "");
        if (!PHONE_PATTERN.matcher(clean).matches()) {
            throw new InvalidArgumentException("Invalid phone number format: " + value);
        }
        this.value = clean;
    }

    public static PhoneNumber of(String value) {
        return new PhoneNumber(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhoneNumber that)) return false;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
