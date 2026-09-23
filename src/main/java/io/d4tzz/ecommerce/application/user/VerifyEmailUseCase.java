package io.d4tzz.ecommerce.application.user;

import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.domain.user.Email;
import io.d4tzz.ecommerce.domain.security.Otp;
import io.d4tzz.ecommerce.domain.security.OtpCode;
import io.d4tzz.ecommerce.domain.security.OtpRepository;
import io.d4tzz.ecommerce.domain.security.OtpType;
import io.d4tzz.ecommerce.domain.user.User;
import io.d4tzz.ecommerce.domain.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VerifyEmailUseCase {

    public record Command(String email, String code) {}

    private final UserRepository userRepository;
    private final OtpRepository otpRepository;

    public VerifyEmailUseCase(UserRepository userRepository, OtpRepository otpRepository) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
    }

    public void execute(Command command) {
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
    }
}
