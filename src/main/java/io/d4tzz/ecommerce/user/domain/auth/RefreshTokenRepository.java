package io.d4tzz.ecommerce.user.domain.auth;

import io.d4tzz.ecommerce.user.domain.UserId;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {
    void save(RefreshToken token);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    boolean revokeIfValid(String tokenHash);

    void revokeFamily(UUID familyId);

    void revokeAllByUserId(UserId userId);
}
