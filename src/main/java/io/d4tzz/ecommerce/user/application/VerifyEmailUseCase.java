package io.d4tzz.ecommerce.user.application;

import io.d4tzz.ecommerce.user.application.command.VerifyEmailCommand;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.user.domain.auth.Otp;
import io.d4tzz.ecommerce.user.domain.auth.OtpCode;
import io.d4tzz.ecommerce.user.domain.auth.OtpRepository;
import io.d4tzz.ecommerce.user.domain.auth.OtpType;
import io.d4tzz.ecommerce.user.domain.Email;
import io.d4tzz.ecommerce.user.domain.User;
import io.d4tzz.ecommerce.user.domain.UserRepository;
import io.d4tzz.ecommerce.user.domain.event.EmailVerifiedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
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

        otp.verify(code);
        otpRepository.save(otp);

        user.verifyEmail();
        userRepository.save(user);

        eventPublisher.publishEvent(new EmailVerifiedEvent(user.getId(), user.getEmail()));
    }
}
