package io.d4tzz.ecommerce.application.user;

import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.domain.user.PasswordHasher;
import io.d4tzz.ecommerce.domain.user.User;
import io.d4tzz.ecommerce.domain.user.UserId;
import io.d4tzz.ecommerce.domain.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ChangePasswordUseCase {

    public record Command(Long userId, String oldPassword, String newPassword) {}

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public ChangePasswordUseCase(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    public void execute(Command command) {
        UserId userId = UserId.of(command.userId());
        User user = userRepository
            .find(userId)
            .orElseThrow(() -> new NotFoundException("User not found with id: " + command.userId()));

        user.changePassword(command.oldPassword(), command.newPassword(), passwordHasher);
        userRepository.save(user);
    }
}
