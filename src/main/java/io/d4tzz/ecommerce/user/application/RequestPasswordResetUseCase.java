package io.d4tzz.ecommerce.user.application;

import io.d4tzz.ecommerce.user.application.command.RequestPasswordResetCommand;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.user.domain.auth.Otp;
import io.d4tzz.ecommerce.user.domain.auth.OtpGenerator;
import io.d4tzz.ecommerce.user.domain.auth.OtpRateLimitedException;
import io.d4tzz.ecommerce.user.domain.auth.OtpRepository;
import io.d4tzz.ecommerce.user.domain.auth.OtpType;
import io.d4tzz.ecommerce.user.domain.Email;
import io.d4tzz.ecommerce.user.domain.User;
import io.d4tzz.ecommerce.user.domain.UserRepository;
import io.d4tzz.ecommerce.user.domain.event.PasswordResetRequestedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Starts a password reset: generates a password-reset OTP for the account and publishes a
 * {@link PasswordResetRequestedEvent} carrying the code. The password itself is changed later by
 * {@link ResetPasswordUseCase}. OTP rate limits apply (see {@link OtpGenerator}).
 */
@Service
@Transactional
public class RequestPasswordResetUseCase {

    private final UserRepository userRepository;
    private final OtpRepository otpRepository;
    private final OtpGenerator otpService;
    private final ApplicationEventPublisher eventPublisher;

    public RequestPasswordResetUseCase(
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
     * @throws OtpRateLimitedException if the OTP was requested too soon after the last one, the previous OTP ran out
     *     of attempts recently, or the daily limit is reached
     */
    public void execute(RequestPasswordResetCommand command) {
        Email email = Email.of(command.email());
        User user = userRepository
            .findByEmail(email)
            .orElseThrow(() -> new NotFoundException("User not found with email: " + command.email()));

        Otp otp = otpService.generateOtp(user.getId(), OtpType.PASSWORD_RESET);
        otpRepository.save(otp);

        eventPublisher.publishEvent(new PasswordResetRequestedEvent(user.getId(), user.getEmail(), otp.getCode()));
    }
}
