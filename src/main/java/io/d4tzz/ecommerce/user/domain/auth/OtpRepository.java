package io.d4tzz.ecommerce.user.domain.auth;

import io.d4tzz.ecommerce.user.domain.*;

import io.d4tzz.ecommerce.common.Repository;
import io.d4tzz.ecommerce.user.domain.UserId;
import java.time.Instant;
import java.util.Optional;

public interface OtpRepository extends Repository<Otp, OtpId> {

    Optional<Otp> findLatestValidOtp(UserId userId, OtpType type);

    Optional<Otp> findLatestOtp(UserId userId, OtpType type);

    void invalidatePreviousOtps(UserId userId, OtpType type);

    long countByUserIdAndTypeSince(UserId userId, OtpType type, Instant since);
}
