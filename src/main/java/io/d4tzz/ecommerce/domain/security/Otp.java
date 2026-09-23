package io.d4tzz.ecommerce.domain.security;

import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.domain.user.UserId;
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

    public void verify(OtpCode inputCode) {
        if (isUsed()) {
            throw new InvalidOperationException("OTP has already been used");
        }
        if (isExpired()) {
            throw new InvalidOperationException("OTP has expired");
        }
        if (isExhausted()) {
            throw new InvalidOperationException("Maximum verification attempts exceeded. Please request a new OTP.");
        }

        if (!code.equals(inputCode)) {
            attempts++;
            if (isExhausted()) {
                throw new InvalidOperationException(
                    "Maximum verification attempts exceeded. Please request a new OTP."
                );
            }
            throw new InvalidArgumentException("Invalid OTP code. Remaining attempts: " + remainingAttempts());
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
