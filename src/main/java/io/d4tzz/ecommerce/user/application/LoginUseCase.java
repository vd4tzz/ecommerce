package io.d4tzz.ecommerce.user.application;

import io.d4tzz.ecommerce.user.application.command.LoginCommand;
import io.d4tzz.ecommerce.user.domain.AccountBlockedException;
import io.d4tzz.ecommerce.user.domain.InvalidCredentialsException;
import io.d4tzz.ecommerce.user.domain.auth.RefreshTokenRepository;
import io.d4tzz.ecommerce.user.domain.auth.TokenPair;
import io.d4tzz.ecommerce.user.domain.auth.TokenProvider;
import io.d4tzz.ecommerce.user.domain.Email;
import io.d4tzz.ecommerce.user.domain.PasswordHasher;
import io.d4tzz.ecommerce.user.domain.User;
import io.d4tzz.ecommerce.user.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logs a user in with email and password and returns a new access token and refresh token (a new token family).
 * <p>
 * An unknown email and a wrong password fail with the same message, so callers cannot tell which emails are
 * registered. The password is checked before the account status, so a blocked account is only reported to someone who
 * knows its password. A successful login records the last login time.
 */
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

    /**
     * @throws InvalidCredentialsException if the email is unknown or the password is wrong
     * @throws AccountBlockedException if the password is right but the account is blocked
     */
    public Response execute(LoginCommand command) {
        Email email = Email.of(command.email());
        User user = userRepository
            .findByEmail(email)
            .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        user.login(command.password(), passwordHasher);
        userRepository.save(user);

        TokenPair tokens = tokenProvider.generateTokens(user);
        refreshTokenRepository.save(tokens.refreshToken());

        return new Response(tokens.accessToken().value(), tokens.refreshToken().getRawToken());
    }
}
