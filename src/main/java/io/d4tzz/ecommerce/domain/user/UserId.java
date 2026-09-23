package io.d4tzz.ecommerce.domain.user;

import io.d4tzz.ecommerce.common.Identifier;

public class UserId extends Identifier<Long> {

    public UserId(Long id) {
        super(id);
    }

    public static UserId of(Long id) {
        return new UserId(id);
    }
}
