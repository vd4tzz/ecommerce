package io.d4tzz.ecommerce.common;

import java.util.Objects;

public class Identifier<T> {

    private T id;

    public Identifier(T id) {
        Objects.requireNonNull(id);
        this.id = id;
    }

    public T value() {
        return id;
    }

    public String toString() {
        return id.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || o.getClass() != this.getClass()) return false;
        return id.equals(((Identifier<?>) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
