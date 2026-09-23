package io.d4tzz.ecommerce.infrastructure.user;

import io.d4tzz.ecommerce.domain.user.HashedPassword;
import io.d4tzz.ecommerce.domain.user.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptPasswordHasher implements PasswordHasher {

    private final PasswordEncoder passwordEncoder;

    public BCryptPasswordHasher() {
        this(new BCryptPasswordEncoder());
    }

    public BCryptPasswordHasher(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public HashedPassword hash(String rawPassword) {
        return HashedPassword.of(passwordEncoder.encode(rawPassword));
    }

    @Override
    public boolean validate(String rawPassword, HashedPassword hashedPassword) {
        return passwordEncoder.matches(rawPassword, hashedPassword.hash());
    }
}
