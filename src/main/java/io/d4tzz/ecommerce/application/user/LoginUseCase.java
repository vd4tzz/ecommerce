package io.d4tzz.ecommerce.application.user;

import io.d4tzz.ecommerce.application.user.command.LoginCommand;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.domain.security.RefreshTokenRepository;
import io.d4tzz.ecommerce.domain.security.TokenPair;
import io.d4tzz.ecommerce.domain.security.TokenProvider;
import io.d4tzz.ecommerce.domain.user.Email;
import io.d4tzz.ecommerce.domain.user.PasswordHasher;
import io.d4tzz.ecommerce.domain.user.User;
import io.d4tzz.ecommerce.domain.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LoginUseCase {

    public record Response(String accessToken, String refreshToken, UserDto user) {}

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

        return new Response(tokens.accessToken().value(), tokens.refreshToken().getRawToken(), UserDto.from(user));
    }
}
