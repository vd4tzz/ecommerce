package io.d4tzz.ecommerce.user.domain;

public class FakePasswordHasher implements PasswordHasher {

    @Override
    public HashedPassword hash(String rawPassword) {
        return HashedPassword.of("hashed:" + rawPassword);
    }

    @Override
    public boolean validate(String rawPassword, HashedPassword hashedPassword) {
        return hashedPassword.hash().equals("hashed:" + rawPassword);
    }
}
