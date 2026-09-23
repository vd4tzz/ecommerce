package io.d4tzz.ecommerce.domain.user;

import io.d4tzz.ecommerce.common.Repository;
import java.util.Optional;

public interface UserRepository extends Repository<User, UserId> {

    Optional<User> findByEmail(Email email);
}
