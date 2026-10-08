package io.d4tzz.ecommerce.user.application;

import io.d4tzz.ecommerce.user.application.command.UpdateProfileCommand;
import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;

import io.d4tzz.ecommerce.user.domain.FullName;
import io.d4tzz.ecommerce.user.domain.PhoneNumber;
import io.d4tzz.ecommerce.user.domain.User;
import io.d4tzz.ecommerce.user.domain.UserId;
import io.d4tzz.ecommerce.user.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Updates the profile of a user. A null full name leaves the current name unchanged, while the phone number and
 * avatar URL are overwritten, so a null clears them.
 */
@Service
@Transactional
public class UpdateProfileUseCase {

    private final UserRepository userRepository;

    public UpdateProfileUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * @throws NotFoundException if the user does not exist
     * @throws InvalidArgumentException if the phone number is malformed
     */
    public void execute(UpdateProfileCommand command) {
        UserId userId = UserId.of(command.userId());
        User user = userRepository
            .find(userId)
            .orElseThrow(() -> new NotFoundException("User not found with id: " + command.userId()));

        FullName fullName = command.fullName() != null ? FullName.of(command.fullName()) : null;
        PhoneNumber phoneNumber = command.phoneNumber() != null ? PhoneNumber.of(command.phoneNumber()) : null;

        user.updateProfile(fullName, phoneNumber, command.avatarUrl());
        userRepository.save(user);
    }
}
