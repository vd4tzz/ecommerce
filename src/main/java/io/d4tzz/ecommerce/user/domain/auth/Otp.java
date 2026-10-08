package io.d4tzz.ecommerce.user.domain.auth;

import io.d4tzz.ecommerce.user.domain.UserId;
import java.time.Duration;
import java.time.Instant;

public class Otp {

    private OtpId id;
    private UserId userId;
    private OtpCode code;
    private OtpType type;
    private int attempts;
    private int maxAttempts;
    private Instant expiresAt;
    private Instant usedAt;
    private Instant createdAt;

    public Otp(
        OtpId id,
        UserId userId,
        OtpCode code,
        OtpType type,
        int attempts,
        int maxAttempts,
        Instant expiresAt,
        Instant usedAt,
        Instant createdAt
    ) {
        this.id = id;
        this.userId = userId;
        this.code = code;
        this.type = type;
        this.attempts = attempts;
        this.maxAttempts = maxAttempts;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
        this.createdAt = createdAt;
    }

    public static Otp generate(UserId userId, OtpType type, Duration validity, int maxAttempts) {
        Instant now = Instant.now();
        return new Otp(null, userId, OtpCode.generate(), type, 0, maxAttempts, now.plus(validity), null, now);
    }

    /**
     * Verifies the submitted code against this OTP and marks it as used on success.
     * <p>
     * A wrong code increments the attempt counter before throwing, so the caller must persist this OTP even when
     * the exception is thrown (and the transaction must not roll back on it), otherwise attempts are never counted.
     *
     * @throws OtpInvalidException if the OTP is already used, expired, or has no attempts left (including when this
     *     very wrong code uses up the last attempt)
     * @throws OtpInvalidCodeException if the code does not match and attempts remain
     */
    public void verify(OtpCode inputCode) {
        if (isUsed()) {
            throw new OtpInvalidException("OTP has already been used");
        }
        if (isExpired()) {
            throw new OtpInvalidException("OTP has expired");
        }
        if (isExhausted()) {
            throw new OtpInvalidException("Maximum verification attempts exceeded. Please request a new OTP.");
        }

        if (!code.equals(inputCode)) {
            attempts++;
            if (isExhausted()) {
                throw new OtpInvalidException("Maximum verification attempts exceeded. Please request a new OTP.");
            }
            throw new OtpInvalidCodeException("Invalid OTP code. Remaining attempts: " + remainingAttempts());
        }

        this.usedAt = Instant.now();
    }

    public void invalidate() {
        this.expiresAt = Instant.now();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isExhausted() {
        return attempts >= maxAttempts;
    }

    public boolean isValid() {
        return !isUsed() && !isExpired() && !isExhausted();
    }

    public int remainingAttempts() {
        return Math.max(0, maxAttempts - attempts);
    }

    public OtpId getId() {
        return id;
    }

    public UserId getUserId() {
        return userId;
    }

    public OtpCode getCode() {
        return code;
    }

    public OtpType getType() {
        return type;
    }

    public int getAttempts() {
        return attempts;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
