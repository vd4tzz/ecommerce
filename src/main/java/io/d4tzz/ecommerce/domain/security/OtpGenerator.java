package io.d4tzz.ecommerce.domain.security;

import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.domain.user.UserId;
import java.time.Duration;
import java.time.Instant;

public class OtpGenerator {

    private static final int DAILY_QUOTA_LIMIT = 5;

    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final Duration PENALTY_LOCKOUT = Duration.ofMinutes(15);

    private static final Duration PASSWORD_RESET_VALIDITY = Duration.ofMinutes(3);
    private static final Duration EMAIL_VERIFICATION_VALIDITY = Duration.ofMinutes(5);

    private static final int PASSWORD_RESET_MAX_ATTEMPTS = 3;
    private static final int EMAIL_VERIFICATION_MAX_ATTEMPTS = 5;

    private final OtpRepository otpRepository;

    public OtpGenerator(OtpRepository otpRepository) {
        this.otpRepository = otpRepository;
    }

    public Otp generateOtp(UserId userId, OtpType type) {
        Instant now = Instant.now();

        otpRepository.findLatestOtp(userId, type).ifPresent(latestOtp -> {
            if (latestOtp.isExhausted()) {
                Instant penaltyEnd = latestOtp.getCreatedAt().plus(PENALTY_LOCKOUT);
                if (now.isBefore(penaltyEnd)) {
                    long waitMinutes = Duration.between(now, penaltyEnd).toMinutes() + 1;
                    throw new InvalidOperationException(
                        "Too many failed attempts. Please try again in " + waitMinutes + " minutes"
                    );
                }
            }

            if (latestOtp.isValid()) {
                Instant cooldownEnd = latestOtp.getCreatedAt().plus(RESEND_COOLDOWN);
                if (now.isBefore(cooldownEnd)) {
                    long remainingSeconds = Duration.between(now, cooldownEnd).toSeconds();
                    throw new InvalidOperationException(
                        "Please wait " + remainingSeconds + " seconds before requesting a new OTP"
                    );
                }
            }
        });

        //  max 5 requests per 24 hours
        Instant oneDayAgo = now.minus(Duration.ofDays(1));
        if (otpRepository.countByUserIdAndTypeSince(userId, type, oneDayAgo) >= DAILY_QUOTA_LIMIT) {
            throw new InvalidOperationException("Daily OTP limit exceeded (maximum 5 requests per day)");
        }

        otpRepository.invalidatePreviousOtps(userId, type);

        Duration validity = resolveValidity(type);
        int maxAttempts = resolveMaxAttempts(type);

        return Otp.generate(userId, type, validity, maxAttempts);
    }

    private Duration resolveValidity(OtpType type) {
        return switch (type) {
            case PASSWORD_RESET -> PASSWORD_RESET_VALIDITY;
            case EMAIL_VERIFICATION -> EMAIL_VERIFICATION_VALIDITY;
        };
    }

    private int resolveMaxAttempts(OtpType type) {
        return switch (type) {
            case PASSWORD_RESET -> PASSWORD_RESET_MAX_ATTEMPTS;
            case EMAIL_VERIFICATION -> EMAIL_VERIFICATION_MAX_ATTEMPTS;
        };
    }
}
