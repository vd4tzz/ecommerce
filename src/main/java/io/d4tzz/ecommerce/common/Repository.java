package io.d4tzz.ecommerce.common;

import java.util.Optional;

public interface Repository<T, ID> {

    Optional<T> find(ID id);

    T save(T entity);

    void delete(ID id);
}
