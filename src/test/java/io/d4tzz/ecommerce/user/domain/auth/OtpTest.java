package io.d4tzz.ecommerce.user.domain.auth;

import static org.junit.jupiter.api.Assertions.*;

import io.d4tzz.ecommerce.user.domain.UserId;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OtpTest {

    private static final OtpCode CODE = OtpCode.of("123456");
    private static final OtpCode WRONG = OtpCode.of("654321");

    private Otp otp(int attempts, int maxAttempts, Instant expiresAt, Instant usedAt) {
        return new Otp(
            OtpId.of(1L),
            UserId.of(1L),
            CODE,
            OtpType.EMAIL_VERIFICATION,
            attempts,
            maxAttempts,
            expiresAt,
            usedAt,
            Instant.now()
        );
    }

    private Otp freshOtp(int maxAttempts) {
        return otp(0, maxAttempts, Instant.now().plus(Duration.ofMinutes(5)), null);
    }

    @Test
    void generate_createsFreshValidOtp() {
        Otp otp = Otp.generate(UserId.of(1L), OtpType.PASSWORD_RESET, Duration.ofMinutes(3), 3);

        assertNull(otp.getId());
        assertEquals(0, otp.getAttempts());
        assertEquals(Duration.ofMinutes(3), Duration.between(otp.getCreatedAt(), otp.getExpiresAt()));
        assertTrue(otp.isValid());
    }

    @Test
    void verify_correctCodeMarksUsed() {
        Otp otp = freshOtp(5);

        otp.verify(CODE);

        assertTrue(otp.isUsed());
        assertFalse(otp.isValid());
        assertEquals(0, otp.getAttempts());
    }

    @Test
    void verify_wrongCodeIncrementsAttemptsAndReportsRemaining() {
        Otp otp = freshOtp(5);

        OtpInvalidCodeException ex = assertThrows(OtpInvalidCodeException.class, () -> otp.verify(WRONG));

        assertEquals(1, otp.getAttempts());
        assertEquals(4, otp.remainingAttempts());
        assertTrue(ex.getMessage().contains("4"));
        assertFalse(otp.isUsed());
    }

    @Test
    void verify_lastWrongAttemptExhaustsOtp() {
        Otp otp = otp(2, 3, Instant.now().plus(Duration.ofMinutes(3)), null);

        assertThrows(OtpInvalidException.class, () -> otp.verify(WRONG));

        assertEquals(3, otp.getAttempts());
        assertTrue(otp.isExhausted());
        assertEquals(0, otp.remainingAttempts());
    }

    @Test
    void verify_exhaustedOtpRejectsEvenCorrectCode() {
        Otp otp = otp(3, 3, Instant.now().plus(Duration.ofMinutes(3)), null);

        assertThrows(OtpInvalidException.class, () -> otp.verify(CODE));
        assertFalse(otp.isUsed());
    }

    @Test
    void verify_usedOtpIsRejected() {
        Otp otp = otp(0, 5, Instant.now().plus(Duration.ofMinutes(3)), Instant.now());

        assertThrows(OtpInvalidException.class, () -> otp.verify(CODE));
    }

    @Test
    void verify_expiredOtpIsRejectedWithoutCountingAttempt() {
        Otp otp = otp(0, 5, Instant.now().minusSeconds(1), null);

        assertThrows(OtpInvalidException.class, () -> otp.verify(WRONG));

        assertTrue(otp.isExpired());
        assertEquals(0, otp.getAttempts());
    }
}
