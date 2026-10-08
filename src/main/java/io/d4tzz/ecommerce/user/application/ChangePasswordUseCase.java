package io.d4tzz.ecommerce.user.application;

import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.user.application.command.ChangePasswordCommand;
import io.d4tzz.ecommerce.user.domain.PasswordHasher;
import io.d4tzz.ecommerce.user.domain.User;
import io.d4tzz.ecommerce.user.domain.UserId;
import io.d4tzz.ecommerce.user.domain.UserRepository;
import io.d4tzz.ecommerce.user.domain.auth.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Changes the password of the signed-in user and signs them out everywhere: all of their refresh tokens are revoked,
 * so other devices must log in again (access tokens already issued stay valid until they expire).
 */
@Service
@Transactional
public class ChangePasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final RefreshTokenRepository refreshTokenRepository;

    public ChangePasswordUseCase(
        UserRepository userRepository,
        PasswordHasher passwordHasher,
        RefreshTokenRepository refreshTokenRepository
    ) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * @throws NotFoundException if the user does not exist
     * @throws InvalidArgumentException if the old password is wrong, or the new password is not 8-128 characters with
     *     at least one uppercase letter, one lowercase letter and one digit
     */
    public void execute(ChangePasswordCommand command) {
        UserId userId = UserId.of(command.userId());
        User user = userRepository
            .find(userId)
            .orElseThrow(() -> new NotFoundException("User not found with id: " + command.userId()));

        user.changePassword(command.oldPassword(), command.newPassword(), passwordHasher);
        userRepository.save(user);
        refreshTokenRepository.revokeAllByUserId(userId);
    }
}
