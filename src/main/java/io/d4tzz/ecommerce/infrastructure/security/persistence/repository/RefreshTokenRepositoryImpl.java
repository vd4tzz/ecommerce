package io.d4tzz.ecommerce.infrastructure.security.persistence.repository;

import io.d4tzz.ecommerce.domain.security.RefreshToken;
import io.d4tzz.ecommerce.domain.security.RefreshTokenId;
import io.d4tzz.ecommerce.domain.security.RefreshTokenRepository;
import io.d4tzz.ecommerce.domain.user.UserId;
import io.d4tzz.ecommerce.infrastructure.security.persistence.entity.JpaRefreshToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
interface SpringRefreshTokenRepository extends JpaRepository<JpaRefreshToken, Long> {

    Optional<JpaRefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.tokenHash = :tokenHash AND r.revoked = false")
    int revokeIfValid(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.familyId = :familyId")
    void revokeFamily(@Param("familyId") UUID familyId);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.userId = :userId")
    void revokeAllByUserId(@Param("userId") Long userId);
}

@Repository
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final SpringRefreshTokenRepository springRepo;

    public RefreshTokenRepositoryImpl(SpringRefreshTokenRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public void save(RefreshToken token) {
        JpaRefreshToken jpa;
        if (token.getId() == null) {
            jpa = toJpa(token);
        } else {
            jpa = springRepo.findById(token.getId().value()).orElseThrow();
            updateJpa(jpa, token);
        }
        springRepo.save(jpa);
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return springRepo.findByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    public boolean revokeIfValid(String tokenHash) {
        return springRepo.revokeIfValid(tokenHash) > 0;
    }

    @Override
    public void revokeFamily(UUID familyId) {
        springRepo.revokeFamily(familyId);
    }

    @Override
    public void revokeAllByUserId(UserId userId) {
        springRepo.revokeAllByUserId(userId.value());
    }

    private RefreshToken toDomain(JpaRefreshToken jpa) {
        return new RefreshToken(
            RefreshTokenId.of(jpa.getId()),
            UserId.of(jpa.getUserId()),
            jpa.getTokenHash(),
            jpa.getFamilyId(),
            jpa.isRevoked(),
            jpa.getExpiresAt(),
            jpa.getCreatedAt()
        );
    }

    private JpaRefreshToken toJpa(RefreshToken token) {
        JpaRefreshToken jpa = new JpaRefreshToken();
        jpa.setUserId(token.getUserId().value());
        jpa.setTokenHash(token.getTokenHash());
        jpa.setFamilyId(token.getFamilyId());
        jpa.setRevoked(token.isRevoked());
        jpa.setExpiresAt(token.getExpiresAt());
        jpa.setCreatedAt(token.getCreatedAt());
        return jpa;
    }

    private void updateJpa(JpaRefreshToken jpa, RefreshToken token) {
        jpa.setRevoked(token.isRevoked());
    }
}
