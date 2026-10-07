package io.d4tzz.ecommerce.user.inbound.rest;

import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.rest.security.CurrentUser;
import io.d4tzz.ecommerce.user.application.ChangePasswordUseCase;
import io.d4tzz.ecommerce.user.application.UpdateProfileUseCase;
import io.d4tzz.ecommerce.user.application.command.ChangePasswordCommand;
import io.d4tzz.ecommerce.user.application.command.UpdateProfileCommand;
import io.d4tzz.ecommerce.user.query.UserQueryService;
import io.d4tzz.ecommerce.user.query.UserSearchCriteria;
import io.d4tzz.ecommerce.user.query.UserView;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UpdateProfileUseCase updateProfileUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final UserQueryService userQueryService;

    public UserController(
        UpdateProfileUseCase updateProfileUseCase,
        ChangePasswordUseCase changePasswordUseCase,
        UserQueryService userQueryService
    ) {
        this.updateProfileUseCase = updateProfileUseCase;
        this.changePasswordUseCase = changePasswordUseCase;
        this.userQueryService = userQueryService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserView> getMe(@CurrentUser Long userId) {
        return ResponseEntity.ok(
            userQueryService.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"))
        );
    }

    @PutMapping("/me/profile")
    public ResponseEntity<Void> updateProfile(
        @CurrentUser Long userId,
        @RequestBody UpdateProfileRequest request
    ) {
        updateProfileUseCase.execute(new UpdateProfileCommand(userId, request.fullName(), request.phoneNumber(), request.avatarUrl()));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
        @CurrentUser Long userId,
        @RequestBody ChangePasswordRequest request
    ) {
        changePasswordUseCase.execute(new ChangePasswordCommand(userId, request.oldPassword(), request.newPassword()));
        return ResponseEntity.noContent().build();
    }

}


record UpdateProfileRequest(String fullName, String phoneNumber, String avatarUrl) {}

record ChangePasswordRequest(String oldPassword, String newPassword) {}
