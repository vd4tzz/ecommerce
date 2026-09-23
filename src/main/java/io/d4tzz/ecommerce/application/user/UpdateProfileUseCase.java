package io.d4tzz.ecommerce.application.user;

import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.domain.user.FullName;
import io.d4tzz.ecommerce.domain.user.PhoneNumber;
import io.d4tzz.ecommerce.domain.user.User;
import io.d4tzz.ecommerce.domain.user.UserId;
import io.d4tzz.ecommerce.domain.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateProfileUseCase {

    public record Command(Long userId, String fullName, String phoneNumber, String avatarUrl) {}

    private final UserRepository userRepository;

    public UpdateProfileUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserDto execute(Command command) {
        UserId userId = UserId.of(command.userId());
        User user = userRepository
            .find(userId)
            .orElseThrow(() -> new NotFoundException("User not found with id: " + command.userId()));

        FullName fullName = command.fullName() != null ? FullName.of(command.fullName()) : null;
        PhoneNumber phoneNumber = command.phoneNumber() != null ? PhoneNumber.of(command.phoneNumber()) : null;

        user.updateProfile(fullName, phoneNumber, command.avatarUrl());
        User saved = userRepository.save(user);

        return UserDto.from(saved);
    }
}
