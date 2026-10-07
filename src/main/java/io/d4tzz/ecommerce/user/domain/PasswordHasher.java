package io.d4tzz.ecommerce.user.domain;

public interface PasswordHasher {

    HashedPassword hash(String rawPassword);

    boolean validate(String rawPassword, HashedPassword hashedPassword);
}
