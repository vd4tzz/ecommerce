package io.d4tzz.ecommerce.user.application;

import io.d4tzz.ecommerce.user.application.command.VerifyEmailCommand;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.user.domain.auth.Otp;
import io.d4tzz.ecommerce.user.domain.auth.OtpCode;
import io.d4tzz.ecommerce.user.domain.auth.OtpInvalidCodeException;
import io.d4tzz.ecommerce.user.domain.auth.OtpInvalidException;
import io.d4tzz.ecommerce.user.domain.auth.OtpRepository;
import io.d4tzz.ecommerce.user.domain.auth.OtpType;
import io.d4tzz.ecommerce.user.domain.Email;
import io.d4tzz.ecommerce.user.domain.User;
import io.d4tzz.ecommerce.user.domain.UserRepository;
import io.d4tzz.ecommerce.user.domain.event.EmailVerifiedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies a user's email with the OTP from {@link SendVerificationOtpUseCase} (or registration), then publishes an
 * {@link EmailVerifiedEvent}.
 * <p>
 * Every attempt is counted, including a wrong code. The OTP is saved even when verification throws, and the
 * transaction is not rolled back on {@link OtpInvalidCodeException} or {@link OtpInvalidException}, otherwise the
 * attempt limit could be bypassed.
 */
@Service
@Transactional(noRollbackFor = { OtpInvalidCodeException.class, OtpInvalidException.class })
public class VerifyEmailUseCase {

    private final UserRepository userRepository;
    private final OtpRepository otpRepository;
    private final ApplicationEventPublisher eventPublisher;

    public VerifyEmailUseCase(
        UserRepository userRepository,
        OtpRepository otpRepository,
        ApplicationEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * @throws InvalidArgumentException if the email or the code is malformed
     * @throws NotFoundException if no user has this email, or the user has no unexpired verification OTP
     * @throws InvalidOperationException if the email is already verified
     * @throws OtpInvalidCodeException if the code is wrong and attempts remain
     * @throws OtpInvalidException if the OTP is already used, expired, or out of attempts (including when this wrong
     *     code uses the last one)
     */
    public void execute(VerifyEmailCommand command) {
        Email email = Email.of(command.email());
        User user = userRepository
            .findByEmail(email)
            .orElseThrow(() -> new NotFoundException("User not found with email: " + command.email()));

        if (user.isEmailVerified()) {
            throw new InvalidOperationException("Email is already verified");
        }

        OtpCode code = OtpCode.of(command.code());
        Otp otp = otpRepository
            .findLatestValidOtp(user.getId(), OtpType.EMAIL_VERIFICATION)
            .orElseThrow(() -> new NotFoundException("No valid OTP found or OTP has expired"));

        try {
            otp.verify(code);
        } finally {
            otpRepository.save(otp);
        }

        user.verifyEmail();
        userRepository.save(user);

        eventPublisher.publishEvent(new EmailVerifiedEvent(user.getId(), user.getEmail()));
    }
}
