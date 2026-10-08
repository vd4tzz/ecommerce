package io.d4tzz.ecommerce.user.application;

import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.user.application.command.ResetPasswordCommand;
import io.d4tzz.ecommerce.user.domain.Email;
import io.d4tzz.ecommerce.user.domain.PasswordHasher;
import io.d4tzz.ecommerce.user.domain.User;
import io.d4tzz.ecommerce.user.domain.UserRepository;
import io.d4tzz.ecommerce.user.domain.auth.Otp;
import io.d4tzz.ecommerce.user.domain.auth.OtpCode;
import io.d4tzz.ecommerce.user.domain.auth.OtpInvalidCodeException;
import io.d4tzz.ecommerce.user.domain.auth.OtpInvalidException;
import io.d4tzz.ecommerce.user.domain.auth.OtpRepository;
import io.d4tzz.ecommerce.user.domain.auth.OtpType;
import io.d4tzz.ecommerce.user.domain.auth.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Completes a password reset with the OTP from {@link RequestPasswordResetUseCase}: checks the code, sets the new
 * password and revokes all of the user's refresh tokens.
 * <p>
 * Every attempt is counted, including a wrong code. The OTP is saved even when verification throws, and the
 * transaction is not rolled back on {@link OtpInvalidCodeException} or {@link OtpInvalidException}, otherwise the
 * attempt limit could be bypassed.
 */
@Service
@Transactional(noRollbackFor = { OtpInvalidCodeException.class, OtpInvalidException.class })
public class ResetPasswordUseCase {

    private final UserRepository userRepository;
    private final OtpRepository otpRepository;
    private final PasswordHasher passwordHasher;
    private final RefreshTokenRepository refreshTokenRepository;

    public ResetPasswordUseCase(
        UserRepository userRepository,
        OtpRepository otpRepository,
        PasswordHasher passwordHasher,
        RefreshTokenRepository refreshTokenRepository
    ) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.passwordHasher = passwordHasher;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * @throws InvalidArgumentException if the email or the code is malformed, or the new password is not 8-128
     *     characters with at least one uppercase letter, one lowercase letter and one digit
     * @throws NotFoundException if no user has this email, or the user has no unexpired password-reset OTP
     * @throws OtpInvalidCodeException if the code is wrong and attempts remain
     * @throws OtpInvalidException if the OTP is already used, expired, or out of attempts (including when this wrong
     *     code uses the last one)
     */
    public void execute(ResetPasswordCommand command) {
        Email email = Email.of(command.email());
        User user = userRepository
            .findByEmail(email)
            .orElseThrow(() -> new NotFoundException("User not found with email: " + command.email()));

        OtpCode code = OtpCode.of(command.code());

        Otp otp = otpRepository
            .findLatestValidOtp(user.getId(), OtpType.PASSWORD_RESET)
            .orElseThrow(() -> new NotFoundException("No valid OTP found or OTP has expired"));

        // otp.verify() can throw exception, must put in try-catch to save otp's state.
        try {
            otp.verify(code);
        } finally {
            otpRepository.save(otp);
        }

        user.resetPassword(command.newPassword(), passwordHasher);
        userRepository.save(user);
        refreshTokenRepository.revokeAllByUserId(user.getId());
    }
}
