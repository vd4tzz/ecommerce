package io.d4tzz.ecommerce.application.user;

import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.domain.user.Email;
import io.d4tzz.ecommerce.domain.security.Otp;
import io.d4tzz.ecommerce.domain.security.OtpCode;
import io.d4tzz.ecommerce.domain.security.OtpRepository;
import io.d4tzz.ecommerce.domain.security.OtpType;
import io.d4tzz.ecommerce.domain.user.PasswordHasher;
import io.d4tzz.ecommerce.domain.user.User;
import io.d4tzz.ecommerce.domain.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ResetPasswordUseCase {

    public record Command(String email, String code, String newPassword) {}

    private final UserRepository userRepository;
    private final OtpRepository otpRepository;
    private final PasswordHasher passwordHasher;

    public ResetPasswordUseCase(
        UserRepository userRepository,
        OtpRepository otpRepository,
        PasswordHasher passwordHasher
    ) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.passwordHasher = passwordHasher;
    }

    public void execute(Command command) {
        Email email = Email.of(command.email());
        User user = userRepository
            .findByEmail(email)
            .orElseThrow(() -> new NotFoundException("User not found with email: " + command.email()));

        OtpCode code = OtpCode.of(command.code());

        Otp otp = otpRepository
            .findLatestValidOtp(user.getId(), OtpType.PASSWORD_RESET)
            .orElseThrow(() -> new NotFoundException("No valid OTP found or OTP has expired"));

        otp.verify(code);
        otpRepository.save(otp);

        user.resetPassword(command.newPassword(), passwordHasher);
        userRepository.save(user);
    }
}
