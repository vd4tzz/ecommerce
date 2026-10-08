package io.d4tzz.ecommerce.user.application;

import static io.d4tzz.ecommerce.user.domain.UserFixtures.PASSWORD;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.d4tzz.ecommerce.user.application.command.LoginCommand;
import io.d4tzz.ecommerce.user.domain.*;
import io.d4tzz.ecommerce.user.domain.auth.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginUseCaseTest {

    private UserRepository userRepository;
    private TokenProvider tokenProvider;
    private RefreshTokenRepository refreshTokenRepository;
    private LoginUseCase useCase;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        tokenProvider = mock(TokenProvider.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        useCase = new LoginUseCase(userRepository, new FakePasswordHasher(), tokenProvider, refreshTokenRepository);
    }

    @Test
    void execute_returnsTokensAndStoresRefreshToken() {
        User user = UserFixtures.activeCustomer();
        RefreshToken refresh = RefreshToken.create(user.getId(), "raw-refresh", Duration.ofDays(7));
        AccessToken access = new AccessToken("access", user.getId(), user.getEmail(), user.getRole(), Instant.now());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(tokenProvider.generateTokens(user)).thenReturn(new TokenPair(access, refresh));

        LoginUseCase.Response response = useCase.execute(new LoginCommand("John@Example.com", PASSWORD));

        assertEquals("access", response.accessToken());
        assertEquals("raw-refresh", response.refreshToken());
        verify(refreshTokenRepository).save(refresh);
        verify(userRepository).save(user);
        assertNotNull(user.getLastLoginAt());
    }

    @Test
    void execute_unknownEmailLooksLikeWrongCredentials() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () ->
            useCase.execute(new LoginCommand("x@example.com", PASSWORD))
        );
        verifyNoInteractions(tokenProvider, refreshTokenRepository);
    }

    @Test
    void execute_wrongPasswordIssuesNoTokens() {
        User user = UserFixtures.activeCustomer();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThrows(InvalidCredentialsException.class, () ->
            useCase.execute(new LoginCommand("john@example.com", "Wrong123"))
        );
        verifyNoInteractions(tokenProvider, refreshTokenRepository);
        verify(userRepository, never()).save(any());
    }

    @Test
    void execute_blockedAccountIsForbidden() {
        User user = UserFixtures.user(UserRole.ROLE_CUSTOMER, UserStatus.BLOCKED, true);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThrows(AccountBlockedException.class, () ->
            useCase.execute(new LoginCommand("john@example.com", PASSWORD))
        );
        verifyNoInteractions(tokenProvider);
    }
}
