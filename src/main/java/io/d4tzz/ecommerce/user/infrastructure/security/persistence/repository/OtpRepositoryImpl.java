package io.d4tzz.ecommerce.user.infrastructure.security.persistence.repository;

import io.d4tzz.ecommerce.user.domain.auth.Otp;
import io.d4tzz.ecommerce.user.domain.auth.OtpCode;
import io.d4tzz.ecommerce.user.domain.auth.OtpId;
import io.d4tzz.ecommerce.user.domain.auth.OtpRepository;
import io.d4tzz.ecommerce.user.domain.auth.OtpType;
import io.d4tzz.ecommerce.user.domain.UserId;
import io.d4tzz.ecommerce.user.infrastructure.security.persistence.entity.JpaOtp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
interface SpringOtpRepository extends JpaRepository<JpaOtp, Long> {

    Optional<JpaOtp> findFirstByUserIdAndTypeOrderByCreatedAtDesc(Long userId, String type);

    Optional<JpaOtp> findFirstByUserIdAndTypeAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
        Long userId,
        String type,
        Instant now
    );

    @Modifying
    @Query(
        """
        UPDATE Otp o
        SET o.expiresAt = :now
        WHERE o.userId = :userId AND o.type = :type AND o.usedAt IS NULL AND o.expiresAt > :now
        """
    )
    void invalidatePreviousOtps(
        @Param("userId") Long userId,
        @Param("type") String type,
        @Param("now") Instant now
    );

    long countByUserIdAndTypeAndCreatedAtGreaterThanEqual(
        Long userId,
        String type,
        Instant since
    );
}

@Repository
public class OtpRepositoryImpl implements OtpRepository {

    private final SpringOtpRepository springRepo;

    public OtpRepositoryImpl(SpringOtpRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public Optional<Otp> find(OtpId id) {
        return springRepo.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<Otp> findLatestValidOtp(UserId userId, OtpType type) {
        return springRepo
            .findFirstByUserIdAndTypeAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                userId.value(),
                type.name(),
                Instant.now()
            )
            .map(this::toDomain);
    }

    @Override
    public Optional<Otp> findLatestOtp(UserId userId, OtpType type) {
        return springRepo
            .findFirstByUserIdAndTypeOrderByCreatedAtDesc(userId.value(), type.name())
            .map(this::toDomain);
    }

    @Override
    public void invalidatePreviousOtps(UserId userId, OtpType type) {
        springRepo.invalidatePreviousOtps(userId.value(), type.name(), Instant.now());
    }

    @Override
    public long countByUserIdAndTypeSince(UserId userId, OtpType type, Instant since) {
        return springRepo.countByUserIdAndTypeAndCreatedAtGreaterThanEqual(
            userId.value(),
            type.name(),
            since
        );
    }

    @Override
    public Otp save(Otp otp) {
        JpaOtp jpaOtp;
        if (otp.getId() == null) {
            jpaOtp = toJpa(otp);
        } else {
            jpaOtp = springRepo.findById(otp.getId().value()).orElseThrow();
            updateJpa(jpaOtp, otp);
        }
        JpaOtp saved = springRepo.save(jpaOtp);
        return toDomain(saved);
    }

    @Override
    public void delete(OtpId id) {
        springRepo.deleteById(id.value());
    }

    private Otp toDomain(JpaOtp jpa) {
        return new Otp(
            OtpId.of(jpa.getId()),
            UserId.of(jpa.getUserId()),
            OtpCode.of(jpa.getCode()),
            OtpType.valueOf(jpa.getType()),
            jpa.getAttempts(),
            jpa.getMaxAttempts(),
            jpa.getExpiresAt(),
            jpa.getUsedAt(),
            jpa.getCreatedAt()
        );
    }

    private JpaOtp toJpa(Otp otp) {
        JpaOtp jpa = new JpaOtp();
        jpa.setUserId(otp.getUserId().value());
        jpa.setCode(otp.getCode().value());
        jpa.setType(otp.getType().name());
        jpa.setAttempts(otp.getAttempts());
        jpa.setMaxAttempts(otp.getMaxAttempts());
        jpa.setExpiresAt(otp.getExpiresAt());
        jpa.setUsedAt(otp.getUsedAt());
        jpa.setCreatedAt(otp.getCreatedAt());
        return jpa;
    }

    private void updateJpa(JpaOtp jpa, Otp otp) {
        jpa.setAttempts(otp.getAttempts());
        jpa.setUsedAt(otp.getUsedAt());
        jpa.setExpiresAt(otp.getExpiresAt());
    }
}
