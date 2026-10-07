package io.d4tzz.ecommerce.user.application;

import io.d4tzz.ecommerce.user.application.command.LoginCommand;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;

import io.d4tzz.ecommerce.user.domain.auth.RefreshTokenRepository;
import io.d4tzz.ecommerce.user.domain.auth.TokenPair;
import io.d4tzz.ecommerce.user.domain.auth.TokenProvider;
import io.d4tzz.ecommerce.user.domain.Email;
import io.d4tzz.ecommerce.user.domain.PasswordHasher;
import io.d4tzz.ecommerce.user.domain.User;
import io.d4tzz.ecommerce.user.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LoginUseCase {

    public record Response(String accessToken, String refreshToken) {}

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    public LoginUseCase(
        UserRepository userRepository,
        PasswordHasher passwordHasher,
        TokenProvider tokenProvider,
        RefreshTokenRepository refreshTokenRepository
    ) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenProvider = tokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public Response execute(LoginCommand command) {
        Email email = Email.of(command.email());
        User user = userRepository
            .findByEmail(email)
            .orElseThrow(() -> new InvalidArgumentException("Invalid email or password"));

        user.login(command.password(), passwordHasher);
        userRepository.save(user);

        TokenPair tokens = tokenProvider.generateTokens(user);
        refreshTokenRepository.save(tokens.refreshToken());

        return new Response(tokens.accessToken().value(), tokens.refreshToken().getRawToken());
    }
}
