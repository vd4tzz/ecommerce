package io.d4tzz.ecommerce.domain.user;

public interface PasswordHasher {

    HashedPassword hash(String rawPassword);

    boolean validate(String rawPassword, HashedPassword hashedPassword);
}
