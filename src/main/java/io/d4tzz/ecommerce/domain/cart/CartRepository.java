package io.d4tzz.ecommerce.domain.cart;

import io.d4tzz.ecommerce.common.Repository;
import io.d4tzz.ecommerce.domain.user.UserId;
import java.util.Optional;

public interface CartRepository extends Repository<Cart, CartId> {

    Optional<Cart> findByUserId(UserId userId);
}
