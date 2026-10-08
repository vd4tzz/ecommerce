package io.d4tzz.ecommerce.user.domain.auth;

import io.d4tzz.ecommerce.user.domain.UserId;
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

    /**
     * Generates a new OTP for the given user and type, enforcing rate-limiting rules:
     * <ul>
     *   <li>If the latest OTP is exhausted and within the penalty window, throws with remaining wait time.</li>
     *   <li>If the latest OTP is still valid and within the resend cooldown (60s), throws with remaining wait time.</li>
     *   <li>If the user has sent 5 or more OTPs in the last 24 hours, throws a daily quota exceeded error.</li>
     * </ul>
     * On success, invalidates all previous OTPs for the user/type and returns a new one (not yet persisted).
     *
     * @throws OtpRateLimitedException if any rate-limiting rule is violated
     */
    public Otp generateOtp(UserId userId, OtpType type) {
        Instant now = Instant.now();

        otpRepository.findLatestOtp(userId, type).ifPresent(latestOtp -> {
            if (latestOtp.isExhausted()) {
                Instant penaltyEnd = latestOtp.getCreatedAt().plus(PENALTY_LOCKOUT);
                if (now.isBefore(penaltyEnd)) {
                    long waitSeconds = Duration.between(now, penaltyEnd).toSeconds() + 1;
                    throw new OtpRateLimitedException(
                        "Too many failed attempts. Please try again in " + waitSeconds + " seconds"
                    );
                }
            }

            if (latestOtp.isValid()) {
                Instant cooldownEnd = latestOtp.getCreatedAt().plus(RESEND_COOLDOWN);
                if (now.isBefore(cooldownEnd)) {
                    long remainingSeconds = Duration.between(now, cooldownEnd).toSeconds();
                    throw new OtpRateLimitedException(
                        "Please wait " + remainingSeconds + " seconds before requesting a new OTP"
                    );
                }
            }
        });

        //  max 5 requests per 24 hours
        Instant oneDayAgo = now.minus(Duration.ofDays(1));
        if (otpRepository.countByUserIdAndTypeSince(userId, type, oneDayAgo) >= DAILY_QUOTA_LIMIT) {
            throw new OtpRateLimitedException("Daily OTP limit exceeded (maximum 5 requests per day)");
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
