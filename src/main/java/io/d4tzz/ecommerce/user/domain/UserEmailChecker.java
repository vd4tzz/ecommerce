package io.d4tzz.ecommerce.user.domain;

public interface UserEmailChecker {

    boolean exists(Email email);
}
