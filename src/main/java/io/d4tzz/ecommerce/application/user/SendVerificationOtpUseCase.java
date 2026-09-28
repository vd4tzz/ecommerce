package io.d4tzz.ecommerce.application.user;

import io.d4tzz.ecommerce.application.user.command.SendVerificationOtpCommand;
import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.domain.security.Otp;
import io.d4tzz.ecommerce.domain.security.OtpGenerator;
import io.d4tzz.ecommerce.domain.security.OtpRepository;
import io.d4tzz.ecommerce.domain.security.OtpType;
import io.d4tzz.ecommerce.domain.user.Email;
import io.d4tzz.ecommerce.domain.user.User;
import io.d4tzz.ecommerce.domain.user.UserRepository;
import io.d4tzz.ecommerce.domain.user.event.EmailVerificationRequestedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
