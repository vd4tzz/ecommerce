package io.d4tzz.ecommerce.user.application;

import io.d4tzz.ecommerce.user.application.command.SendVerificationOtpCommand;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.user.domain.auth.Otp;
import io.d4tzz.ecommerce.user.domain.auth.OtpGenerator;
import io.d4tzz.ecommerce.user.domain.auth.OtpRateLimitedException;
import io.d4tzz.ecommerce.user.domain.auth.OtpRepository;
import io.d4tzz.ecommerce.user.domain.auth.OtpType;
import io.d4tzz.ecommerce.user.domain.Email;
import io.d4tzz.ecommerce.user.domain.User;
import io.d4tzz.ecommerce.user.domain.UserRepository;
import io.d4tzz.ecommerce.user.domain.event.EmailVerificationRequestedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sends a new email-verification OTP to an account whose email is not verified yet, and publishes an
 * {@link EmailVerificationRequestedEvent} carrying the code. OTP rate limits apply (see {@link OtpGenerator}).
 */
@Service
@Transactional
public class SendVerificationOtpUseCase {

    private final UserRepository userRepository;
    private final OtpRepository otpRepository;
    private final OtpGenerator otpService;
    private final ApplicationEventPublisher eventPublisher;

    public SendVerificationOtpUseCase(
        UserRepository userRepository,
        OtpRepository otpRepository,
        OtpGenerator otpService,
        ApplicationEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.otpService = otpService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * @throws InvalidArgumentException if the email is blank or malformed
     * @throws NotFoundException if no user has this email
     * @throws InvalidOperationException if the email is already verified
     * @throws OtpRateLimitedException if the OTP was requested too soon after the last one, the previous OTP ran out
     *     of attempts recently, or the daily limit is reached
     */
    public void execute(SendVerificationOtpCommand command) {
        Email email = Email.of(command.email());
        User user = userRepository
            .findByEmail(email)
            .orElseThrow(() -> new NotFoundException("User not found with email: " + command.email()));

        if (user.isEmailVerified()) {
            throw new InvalidOperationException("Email is already verified");
        }

        Otp otp = otpService.generateOtp(user.getId(), OtpType.EMAIL_VERIFICATION);
        otpRepository.save(otp);

        eventPublisher.publishEvent(new EmailVerificationRequestedEvent(user.getId(), user.getEmail(), otp.getCode()));
    }
}
