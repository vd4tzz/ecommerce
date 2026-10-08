package io.d4tzz.ecommerce.user.application;

import io.d4tzz.ecommerce.user.application.command.RefreshTokenCommand;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.user.domain.AccountBlockedException;
import io.d4tzz.ecommerce.user.domain.auth.InvalidRefreshTokenException;
import io.d4tzz.ecommerce.user.domain.auth.RefreshTokenReusedException;
import io.d4tzz.ecommerce.user.domain.auth.RefreshToken;
import io.d4tzz.ecommerce.user.domain.auth.RefreshTokenRepository;
import io.d4tzz.ecommerce.user.domain.auth.TokenPair;
import io.d4tzz.ecommerce.user.domain.auth.TokenProvider;
import io.d4tzz.ecommerce.user.domain.User;
import io.d4tzz.ecommerce.user.domain.UserRepository;
import io.d4tzz.ecommerce.user.domain.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exchanges a refresh token for a new access token and a new refresh token (rotation). Each refresh token can be used
 * once: using it revokes it and issues its successor in the same token family.
 * <p>
 * Presenting a token that was already revoked, or losing a race to revoke it, is treated as theft: the whole family
 * is revoked so every session descended from that login is signed out. This revocation must survive the error, so the
 * transaction is not rolled back on {@link RefreshTokenReusedException}.
 * <p>
 * Checks run in this order: blank token, unknown token, already revoked, expired, atomic revoke, user lookup, blocked
 * status.
 */
@Service
@Transactional(noRollbackFor = RefreshTokenReusedException.class)
public class RefreshTokenUseCase {

    public record Response(String accessToken, String refreshToken) {}

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;

    public RefreshTokenUseCase(
        RefreshTokenRepository refreshTokenRepository,
        UserRepository userRepository,
        TokenProvider tokenProvider
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
    }

    /**
     * @throws InvalidArgumentException if the token is null or blank
     * @throws InvalidRefreshTokenException if the token is unknown or has expired
     * @throws RefreshTokenReusedException if the token was already revoked or used, in which case its whole family is
     *     revoked
     * @throws NotFoundException if the token's user no longer exists
     * @throws AccountBlockedException if the user is blocked
     */
    public Response execute(RefreshTokenCommand command) {
        if (command.refreshToken() == null || command.refreshToken().isBlank()) {
            throw new InvalidArgumentException("Refresh token must not be blank");
        }

        String tokenHash = RefreshToken.hash(command.refreshToken());
        RefreshToken token = refreshTokenRepository
            .findByTokenHash(tokenHash)
            .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        if (token.isRevoked()) {
            // Reuse detection: revoke all tokens in the same family
            refreshTokenRepository.revokeFamily(token.getFamilyId());
            throw new RefreshTokenReusedException("Token reuse detected. All sessions have been revoked.");
        }

        if (token.isExpired()) {
            throw new InvalidRefreshTokenException("Refresh token has expired");
        }

        boolean revoked = refreshTokenRepository.revokeIfValid(tokenHash);
        if (!revoked) {
            refreshTokenRepository.revokeFamily(token.getFamilyId());
            throw new RefreshTokenReusedException("Token has already been used or revoked");
        }

        User user = userRepository
            .find(token.getUserId())
            .orElseThrow(() -> new NotFoundException("User not found with id: " + token.getUserId().value()));

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new AccountBlockedException("Account has been blocked");
        }

        TokenPair newTokens = tokenProvider.rotate(token, user);
        refreshTokenRepository.save(newTokens.refreshToken());

        return new Response(
            newTokens.accessToken().value(),
            newTokens.refreshToken().getRawToken()
        );
    }
}
