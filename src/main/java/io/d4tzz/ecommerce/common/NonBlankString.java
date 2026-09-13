package io.d4tzz.ecommerce.common;

import java.util.Objects;

public class NonBlankString {

    private String value;

    public NonBlankString(String value) {
        Objects.requireNonNull(value);
        if (value.isBlank()) {
            throw new IllegalArgumentException("String cannot be blank");
        }
        this.value = value;
    }

    public String value() {
        return this.value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || o.getClass() != this.getClass()) return false;
        return this.value.equals(((NonBlankString) o).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
