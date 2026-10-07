package io.d4tzz.ecommerce.user.application;

import io.d4tzz.ecommerce.user.application.command.ResetPasswordCommand;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.user.domain.auth.Otp;
import io.d4tzz.ecommerce.user.domain.auth.OtpCode;
import io.d4tzz.ecommerce.user.domain.auth.OtpRepository;
import io.d4tzz.ecommerce.user.domain.auth.OtpType;
import io.d4tzz.ecommerce.user.domain.Email;
import io.d4tzz.ecommerce.user.domain.PasswordHasher;
import io.d4tzz.ecommerce.user.domain.User;
import io.d4tzz.ecommerce.user.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ResetPasswordUseCase {

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

    public void execute(ResetPasswordCommand command) {
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
