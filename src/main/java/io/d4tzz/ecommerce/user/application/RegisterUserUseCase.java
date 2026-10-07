package io.d4tzz.ecommerce.user.application;

import io.d4tzz.ecommerce.user.application.command.RegisterUserCommand;
import io.d4tzz.ecommerce.user.domain.UserId;
import io.d4tzz.ecommerce.user.domain.auth.Otp;
import io.d4tzz.ecommerce.user.domain.auth.OtpGenerator;
import io.d4tzz.ecommerce.user.domain.auth.OtpRepository;
import io.d4tzz.ecommerce.user.domain.auth.OtpType;
import io.d4tzz.ecommerce.user.domain.Email;
import io.d4tzz.ecommerce.user.domain.FullName;
import io.d4tzz.ecommerce.user.domain.PasswordHasher;
import io.d4tzz.ecommerce.user.domain.User;
import io.d4tzz.ecommerce.user.domain.UserRepository;
import io.d4tzz.ecommerce.user.domain.event.UserRegisteredEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RegisterUserUseCase {

    private final UserRepository userRepository;
    private final OtpRepository otpRepository;
    private final OtpGenerator otpService;
    private final PasswordHasher passwordHasher;
    private final ApplicationEventPublisher eventPublisher;

    public RegisterUserUseCase(
        UserRepository userRepository,
        OtpRepository otpRepository,
        OtpGenerator otpService,
        PasswordHasher passwordHasher,
        ApplicationEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.otpService = otpService;
        this.passwordHasher = passwordHasher;
        this.eventPublisher = eventPublisher;
    }

    public UserId execute(RegisterUserCommand command) {
        User user = User.register(
            Email.of(command.email()),
            command.password(),
            FullName.of(command.fullName()),
            email -> userRepository.findByEmail(email).isPresent(),
            passwordHasher
        );

        user = userRepository.save(user);

        Otp otp = otpService.generateOtp(user.getId(), OtpType.EMAIL_VERIFICATION);
        otpRepository.save(otp);

        eventPublisher.publishEvent(new UserRegisteredEvent(user.getId(), user.getEmail(), otp.getCode()));

        return user.getId();
    }
}
