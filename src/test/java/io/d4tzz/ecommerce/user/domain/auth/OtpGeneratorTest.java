package io.d4tzz.ecommerce.user.domain.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.d4tzz.ecommerce.user.domain.UserId;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OtpGeneratorTest {

    private static final UserId USER = UserId.of(1L);

    private OtpRepository repository;
    private OtpGenerator generator;

    @BeforeEach
    void setUp() {
        repository = mock(OtpRepository.class);
        generator = new OtpGenerator(repository);
    }

    private Otp existing(Instant createdAt, int attempts, int maxAttempts, Instant expiresAt) {
        return new Otp(
            OtpId.of(1L),
            USER,
            OtpCode.of("111111"),
            OtpType.EMAIL_VERIFICATION,
            attempts,
            maxAttempts,
            expiresAt,
            null,
            createdAt
        );
    }

    @Test
    void generate_firstOtpForEmailVerification() {
        Otp otp = generator.generateOtp(USER, OtpType.EMAIL_VERIFICATION);

        assertEquals(5, otp.getMaxAttempts());
        assertEquals(Duration.ofMinutes(5), Duration.between(otp.getCreatedAt(), otp.getExpiresAt()));
        verify(repository).invalidatePreviousOtps(USER, OtpType.EMAIL_VERIFICATION);
    }

    @Test
    void generate_passwordResetHasShorterValidityAndFewerAttempts() {
        Otp otp = generator.generateOtp(USER, OtpType.PASSWORD_RESET);

        assertEquals(3, otp.getMaxAttempts());
        assertEquals(Duration.ofMinutes(3), Duration.between(otp.getCreatedAt(), otp.getExpiresAt()));
    }

    @Test
    void generate_rejectsWithinResendCooldownOfValidOtp() {
        Instant now = Instant.now();
        when(repository.findLatestOtp(USER, OtpType.EMAIL_VERIFICATION))
            .thenReturn(Optional.of(existing(now.minusSeconds(10), 0, 5, now.plusSeconds(290))));

        assertThrows(OtpRateLimitedException.class, () -> generator.generateOtp(USER, OtpType.EMAIL_VERIFICATION));
        verify(repository, never()).invalidatePreviousOtps(any(), any());
    }

    @Test
    void generate_allowsAfterResendCooldown() {
        Instant now = Instant.now();
        when(repository.findLatestOtp(USER, OtpType.EMAIL_VERIFICATION))
            .thenReturn(Optional.of(existing(now.minusSeconds(61), 0, 5, now.plusSeconds(239))));

        assertNotNull(generator.generateOtp(USER, OtpType.EMAIL_VERIFICATION));
    }

    @Test
    void generate_rejectsWithinPenaltyAfterExhaustedOtp() {
        Instant now = Instant.now();
        when(repository.findLatestOtp(USER, OtpType.EMAIL_VERIFICATION))
            .thenReturn(Optional.of(existing(now.minus(Duration.ofMinutes(5)), 5, 5, now.minusSeconds(1))));

        assertThrows(OtpRateLimitedException.class, () -> generator.generateOtp(USER, OtpType.EMAIL_VERIFICATION));
    }

    @Test
    void generate_allowsAfterPenaltyWindow() {
        Instant now = Instant.now();
        when(repository.findLatestOtp(USER, OtpType.EMAIL_VERIFICATION))
            .thenReturn(Optional.of(existing(now.minus(Duration.ofMinutes(16)), 5, 5, now.minus(Duration.ofMinutes(11)))));

        assertNotNull(generator.generateOtp(USER, OtpType.EMAIL_VERIFICATION));
    }

    @Test
    void generate_rejectsWhenDailyQuotaReached() {
        when(repository.countByUserIdAndTypeSince(eq(USER), eq(OtpType.PASSWORD_RESET), any(Instant.class)))
            .thenReturn(5L);

        assertThrows(OtpRateLimitedException.class, () -> generator.generateOtp(USER, OtpType.PASSWORD_RESET));
        verify(repository, never()).invalidatePreviousOtps(any(), any());
    }

    @Test
    void generate_allowsBelowDailyQuota() {
        when(repository.countByUserIdAndTypeSince(eq(USER), eq(OtpType.PASSWORD_RESET), any(Instant.class)))
            .thenReturn(4L);

        assertNotNull(generator.generateOtp(USER, OtpType.PASSWORD_RESET));
    }
}
