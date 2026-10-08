package io.d4tzz.ecommerce.user.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.user.application.command.RefreshTokenCommand;
import io.d4tzz.ecommerce.user.domain.*;
import io.d4tzz.ecommerce.user.domain.auth.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class RefreshTokenUseCaseTest {

    private static final String RAW = "raw-token";
    private static final String HASH = RefreshToken.hash(RAW);

    private RefreshTokenRepository refreshTokenRepository;
    private UserRepository userRepository;
    private TokenProvider tokenProvider;
    private RefreshTokenUseCase useCase;

    @BeforeEach
    void setUp() {
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        userRepository = mock(UserRepository.class);
        tokenProvider = mock(TokenProvider.class);
        useCase = new RefreshTokenUseCase(refreshTokenRepository, userRepository, tokenProvider);
    }

    private RefreshToken stored(boolean revoked, Instant expiresAt) {
        return new RefreshToken(
            RefreshTokenId.of(1L), UserId.of(1L), HASH, UUID.randomUUID(), revoked, expiresAt, Instant.now()
        );
    }

    private RefreshToken validStored() {
        return stored(false, Instant.now().plus(Duration.ofDays(1)));
    }

    @Test
    void execute_blankTokenIsRejected() {
        assertThrows(InvalidArgumentException.class, () -> useCase.execute(new RefreshTokenCommand(" ")));
        assertThrows(InvalidArgumentException.class, () -> useCase.execute(new RefreshTokenCommand(null)));
    }

    @Test
    void execute_unknownTokenIsInvalid() {
        when(refreshTokenRepository.findByTokenHash(HASH)).thenReturn(Optional.empty());

        assertThrows(InvalidRefreshTokenException.class, () -> useCase.execute(new RefreshTokenCommand(RAW)));
    }

    @Test
    void execute_revokedTokenRevokesWholeFamily() {
        RefreshToken token = stored(true, Instant.now().plus(Duration.ofDays(1)));
        when(refreshTokenRepository.findByTokenHash(HASH)).thenReturn(Optional.of(token));

        assertThrows(RefreshTokenReusedException.class, () -> useCase.execute(new RefreshTokenCommand(RAW)));
        verify(refreshTokenRepository).revokeFamily(token.getFamilyId());
    }

    @Test
    void execute_expiredTokenIsInvalid() {
        when(refreshTokenRepository.findByTokenHash(HASH))
            .thenReturn(Optional.of(stored(false, Instant.now().minusSeconds(1))));

        assertThrows(InvalidRefreshTokenException.class, () -> useCase.execute(new RefreshTokenCommand(RAW)));
        verify(refreshTokenRepository, never()).revokeIfValid(any());
    }

    @Test
    void execute_lostRevocationRaceRevokesFamily() {
        RefreshToken token = validStored();
        when(refreshTokenRepository.findByTokenHash(HASH)).thenReturn(Optional.of(token));
        when(refreshTokenRepository.revokeIfValid(HASH)).thenReturn(false);

        assertThrows(RefreshTokenReusedException.class, () -> useCase.execute(new RefreshTokenCommand(RAW)));
        verify(refreshTokenRepository).revokeFamily(token.getFamilyId());
    }

    @Test
    void execute_blockedUserIsForbidden() {
        when(refreshTokenRepository.findByTokenHash(HASH)).thenReturn(Optional.of(validStored()));
        when(refreshTokenRepository.revokeIfValid(HASH)).thenReturn(true);
        when(userRepository.find(UserId.of(1L)))
            .thenReturn(Optional.of(UserFixtures.user(UserRole.ROLE_CUSTOMER, UserStatus.BLOCKED, true)));

        assertThrows(AccountBlockedException.class, () -> useCase.execute(new RefreshTokenCommand(RAW)));
        verifyNoInteractions(tokenProvider);
    }

    @Test
    void execute_rotatesAndStoresNewToken() {
        RefreshToken token = validStored();
        User user = UserFixtures.activeCustomer();
        RefreshToken rotated = RefreshToken.create(user.getId(), "new-raw", Duration.ofDays(7));
        AccessToken access = new AccessToken("new-access", user.getId(), user.getEmail(), user.getRole(), Instant.now());
        when(refreshTokenRepository.findByTokenHash(HASH)).thenReturn(Optional.of(token));
        when(refreshTokenRepository.revokeIfValid(HASH)).thenReturn(true);
        when(userRepository.find(user.getId())).thenReturn(Optional.of(user));
        when(tokenProvider.rotate(token, user)).thenReturn(new TokenPair(access, rotated));

        RefreshTokenUseCase.Response response = useCase.execute(new RefreshTokenCommand(RAW));

        assertEquals("new-access", response.accessToken());
        assertEquals("new-raw", response.refreshToken());
        verify(refreshTokenRepository).save(rotated);
    }

    @Test
    void reuseDetectionMustNotBeRolledBack() {
        Transactional tx = RefreshTokenUseCase.class.getAnnotation(Transactional.class);

        assertTrue(java.util.List.of(tx.noRollbackFor()).contains(RefreshTokenReusedException.class));
    }
}
