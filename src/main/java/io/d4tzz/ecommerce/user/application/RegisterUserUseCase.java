package io.d4tzz.ecommerce.user.application;

import io.d4tzz.ecommerce.user.application.command.RegisterUserCommand;
import io.d4tzz.ecommerce.user.domain.EmailAlreadyExistsException;
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
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;

/**
 * Registers a new customer account and starts email verification.
 * <p>
 * The account is created active but with an unverified email. An email-verification OTP is generated and saved, and a
 * {@link UserRegisteredEvent} carrying the code is published. The user's password is stored hashed.
 *
 * @see VerifyEmailUseCase
 */
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

    /**
     * @return the id of the new user
     * @throws InvalidArgumentException if the email is blank or malformed, or the password is not 8-128 characters
     *     with at least one uppercase letter, one lowercase letter and one digit
     * @throws EmailAlreadyExistsException if the email is already registered
     */
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
