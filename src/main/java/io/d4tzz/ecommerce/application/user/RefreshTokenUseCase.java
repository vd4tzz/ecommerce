package io.d4tzz.ecommerce.application.user;

import io.d4tzz.ecommerce.application.user.command.RefreshTokenCommand;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.domain.security.RefreshToken;
import io.d4tzz.ecommerce.domain.security.RefreshTokenRepository;
import io.d4tzz.ecommerce.domain.security.TokenPair;
import io.d4tzz.ecommerce.domain.security.TokenProvider;
import io.d4tzz.ecommerce.domain.user.User;
import io.d4tzz.ecommerce.domain.user.UserRepository;
import io.d4tzz.ecommerce.domain.user.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
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

    public Response execute(RefreshTokenCommand command) {
        if (command.refreshToken() == null || command.refreshToken().isBlank()) {
            throw new InvalidArgumentException("Refresh token must not be blank");
        }

        String tokenHash = RefreshToken.hash(command.refreshToken());
        RefreshToken token = refreshTokenRepository
            .findByTokenHash(tokenHash)
            .orElseThrow(() -> new NotFoundException("Invalid refresh token"));

        if (token.isRevoked()) {
            // Reuse detection: revoke all tokens in the same family
            refreshTokenRepository.revokeFamily(token.getFamilyId());
            throw new InvalidOperationException("Token reuse detected. All sessions have been revoked.");
        }

        if (token.isExpired()) {
            throw new InvalidOperationException("Refresh token has expired");
        }

        boolean revoked = refreshTokenRepository.revokeIfValid(tokenHash);
        if (!revoked) {
            refreshTokenRepository.revokeFamily(token.getFamilyId());
            throw new InvalidOperationException("Token has already been used or revoked");
        }

        User user = userRepository
            .find(token.getUserId())
            .orElseThrow(() -> new NotFoundException("User not found with id: " + token.getUserId().value()));

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new InvalidOperationException("Account has been blocked");
        }

        TokenPair newTokens = tokenProvider.rotate(token, user);
        refreshTokenRepository.save(newTokens.refreshToken());

        return new Response(
            newTokens.accessToken().value(),
            newTokens.refreshToken().getRawToken()
        );
    }
}
