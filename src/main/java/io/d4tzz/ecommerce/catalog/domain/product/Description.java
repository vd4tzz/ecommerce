package io.d4tzz.ecommerce.catalog.domain.product;

import java.util.Objects;

public class Description {

    private final String value;

    public Description(String value) {
        this.value = value != null ? value : "";
    }

    public static Description of(String value) {
        return new Description(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Description that = (Description) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
