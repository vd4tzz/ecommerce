package io.d4tzz.ecommerce.user.application;

import static io.d4tzz.ecommerce.user.domain.UserFixtures.PASSWORD;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.user.application.command.ResetPasswordCommand;
import io.d4tzz.ecommerce.user.application.command.VerifyEmailCommand;
import io.d4tzz.ecommerce.user.domain.*;
import io.d4tzz.ecommerce.user.domain.auth.*;
import io.d4tzz.ecommerce.user.domain.event.EmailVerifiedEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

class OtpVerificationUseCasesTest {

    private static final String CODE = "123456";
    private static final String WRONG = "654321";

    private UserRepository userRepository;
    private OtpRepository otpRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private ApplicationEventPublisher eventPublisher;
    private VerifyEmailUseCase verifyEmail;
    private ResetPasswordUseCase resetPassword;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        otpRepository = mock(OtpRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        verifyEmail = new VerifyEmailUseCase(userRepository, otpRepository, eventPublisher);
        resetPassword = new ResetPasswordUseCase(
            userRepository, otpRepository, new FakePasswordHasher(), refreshTokenRepository
        );
    }

    private Otp otp(OtpType type, int attempts, int maxAttempts) {
        return new Otp(
            OtpId.of(1L), UserId.of(1L), OtpCode.of(CODE), type, attempts, maxAttempts,
            Instant.now().plus(Duration.ofMinutes(3)), null, Instant.now()
        );
    }

    private User givenUser(boolean emailVerified) {
        User user = UserFixtures.user(UserRole.ROLE_CUSTOMER, UserStatus.ACTIVE, emailVerified);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        return user;
    }

    // ---- VerifyEmailUseCase ----

    @Test
    void verifyEmail_success() {
        User user = givenUser(false);
        Otp otp = otp(OtpType.EMAIL_VERIFICATION, 0, 5);
        when(otpRepository.findLatestValidOtp(user.getId(), OtpType.EMAIL_VERIFICATION)).thenReturn(Optional.of(otp));

        verifyEmail.execute(new VerifyEmailCommand("john@example.com", CODE));

        assertTrue(user.isEmailVerified());
        assertTrue(otp.isUsed());
        verify(otpRepository).save(otp);
        verify(userRepository).save(user);
        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertInstanceOf(EmailVerifiedEvent.class, event.getValue());
    }

    @Test
    void verifyEmail_wrongCodePersistsAttemptAndDoesNotVerify() {
        User user = givenUser(false);
        Otp otp = otp(OtpType.EMAIL_VERIFICATION, 0, 5);
        when(otpRepository.findLatestValidOtp(user.getId(), OtpType.EMAIL_VERIFICATION)).thenReturn(Optional.of(otp));

        assertThrows(OtpInvalidCodeException.class, () -> verifyEmail.execute(new VerifyEmailCommand("john@example.com", WRONG)));

        assertEquals(1, otp.getAttempts());
        verify(otpRepository).save(otp);
        assertFalse(user.isEmailVerified());
        verify(userRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void verifyEmail_alreadyVerifiedIsRejected() {
        givenUser(true);

        assertThrows(InvalidOperationException.class, () -> verifyEmail.execute(new VerifyEmailCommand("john@example.com", CODE)));
        verifyNoInteractions(otpRepository);
    }

    @Test
    void verifyEmail_noValidOtp() {
        User user = givenUser(false);
        when(otpRepository.findLatestValidOtp(user.getId(), OtpType.EMAIL_VERIFICATION)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> verifyEmail.execute(new VerifyEmailCommand("john@example.com", CODE)));
    }

    @Test
    void verifyEmail_unknownUser() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> verifyEmail.execute(new VerifyEmailCommand("x@example.com", CODE)));
    }

    // ---- ResetPasswordUseCase ----

    @Test
    void resetPassword_success() {
        User user = givenUser(true);
        Otp otp = otp(OtpType.PASSWORD_RESET, 0, 3);
        when(otpRepository.findLatestValidOtp(user.getId(), OtpType.PASSWORD_RESET)).thenReturn(Optional.of(otp));

        resetPassword.execute(new ResetPasswordCommand("john@example.com", CODE, "NewSecret456"));

        assertEquals(new FakePasswordHasher().hash("NewSecret456"), user.getPassword());
        assertTrue(otp.isUsed());
        verify(userRepository).save(user);
        verify(refreshTokenRepository).revokeAllByUserId(user.getId());
    }

    @Test
    void resetPassword_wrongCodePersistsAttemptAndKeepsPassword() {
        User user = givenUser(true);
        Otp otp = otp(OtpType.PASSWORD_RESET, 0, 3);
        when(otpRepository.findLatestValidOtp(user.getId(), OtpType.PASSWORD_RESET)).thenReturn(Optional.of(otp));

        assertThrows(
            OtpInvalidCodeException.class,
            () -> resetPassword.execute(new ResetPasswordCommand("john@example.com", WRONG, "NewSecret456"))
        );

        assertEquals(1, otp.getAttempts());
        verify(otpRepository).save(otp);
        assertEquals(new FakePasswordHasher().hash(PASSWORD), user.getPassword());
        verify(userRepository, never()).save(any());
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void resetPassword_exhaustingOtpIsAlsoPersisted() {
        User user = givenUser(true);
        Otp otp = otp(OtpType.PASSWORD_RESET, 2, 3);
        when(otpRepository.findLatestValidOtp(user.getId(), OtpType.PASSWORD_RESET)).thenReturn(Optional.of(otp));

        assertThrows(
            OtpInvalidException.class,
            () -> resetPassword.execute(new ResetPasswordCommand("john@example.com", WRONG, "NewSecret456"))
        );

        assertTrue(otp.isExhausted());
        verify(otpRepository).save(otp);
    }

    @Test
    void otpAttemptsMustNotBeRolledBack() {
        for (Class<?> useCase : List.of(VerifyEmailUseCase.class, ResetPasswordUseCase.class)) {
            List<Class<? extends Throwable>> noRollback = List.of(useCase.getAnnotation(Transactional.class).noRollbackFor());
            assertTrue(noRollback.contains(OtpInvalidCodeException.class), useCase.getSimpleName());
            assertTrue(noRollback.contains(OtpInvalidException.class), useCase.getSimpleName());
        }
    }
}
