package io.d4tzz.ecommerce.domain.user;

public interface UserEmailChecker {

    boolean exists(Email email);
}
