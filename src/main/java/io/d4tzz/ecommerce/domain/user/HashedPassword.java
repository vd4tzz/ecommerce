package io.d4tzz.ecommerce.domain.user;

import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import java.util.Objects;

public class HashedPassword {

    private final String hash;

    public HashedPassword(String hash) {
        if (hash == null || hash.isBlank()) {
            throw new InvalidArgumentException("Password hash must not be blank");
        }
        this.hash = hash.trim();
    }

    public static HashedPassword of(String hash) {
        return new HashedPassword(hash);
    }

    public String hash() {
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HashedPassword that)) return false;
        return Objects.equals(hash, that.hash);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(hash);
    }
}
