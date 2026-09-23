package io.d4tzz.ecommerce.rest;

import io.d4tzz.ecommerce.application.user.ChangePasswordUseCase;
import io.d4tzz.ecommerce.application.user.UpdateProfileUseCase;
import io.d4tzz.ecommerce.application.user.UserDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UpdateProfileUseCase updateProfileUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;

    public UserController(
        UpdateProfileUseCase updateProfileUseCase,
        ChangePasswordUseCase changePasswordUseCase
    ) {
        this.updateProfileUseCase = updateProfileUseCase;
        this.changePasswordUseCase = changePasswordUseCase;
    }

    @PutMapping("/{id}/profile")
    public ResponseEntity<UserDto> updateProfile(
        @PathVariable Long id,
        @RequestBody UpdateProfileRequest request
    ) {
        var command = new UpdateProfileUseCase.Command(id, request.fullName(), request.phoneNumber(), request.avatarUrl());
        UserDto response = updateProfileUseCase.execute(command);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<Void> changePassword(
        @PathVariable Long id,
        @RequestBody ChangePasswordRequest request
    ) {
        var command = new ChangePasswordUseCase.Command(id, request.oldPassword(), request.newPassword());
        changePasswordUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }
}

record UpdateProfileRequest(String fullName, String phoneNumber, String avatarUrl) {}

record ChangePasswordRequest(String oldPassword, String newPassword) {}
