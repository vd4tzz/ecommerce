package io.d4tzz.ecommerce.user.inbound.rest;

import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.spring.SpringValidation;
import io.d4tzz.ecommerce.spring.security.CurrentUser;
import io.d4tzz.ecommerce.user.application.ChangePasswordUseCase;
import io.d4tzz.ecommerce.user.application.UpdateProfileUseCase;
import io.d4tzz.ecommerce.user.application.command.ChangePasswordCommand;
import io.d4tzz.ecommerce.user.application.command.UpdateProfileCommand;
import io.d4tzz.ecommerce.user.query.UserQueryService;
import io.d4tzz.ecommerce.user.query.UserView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "The authenticated user's own account")
public class UserController {

    private final UpdateProfileUseCase updateProfileUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final UserQueryService userQueryService;
    private final Validator validator;

    public UserController(
        UpdateProfileUseCase updateProfileUseCase,
        ChangePasswordUseCase changePasswordUseCase,
        UserQueryService userQueryService,
        Validator validator
    ) {
        this.updateProfileUseCase = updateProfileUseCase;
        this.changePasswordUseCase = changePasswordUseCase;
        this.userQueryService = userQueryService;
        this.validator = validator;
    }

    @Operation(
        summary = "Get my profile",
        description = "Returns the authenticated user's account details."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Current user"),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @GetMapping("/me")
    public ResponseEntity<UserView> getMe(@CurrentUser Long userId) {
        return ResponseEntity.ok(
            userQueryService.findById(userId).orElseThrow(() -> new NotFoundException("User not found"))
        );
    }

    @Operation(
        summary = "Update my profile",
        description = "Updates full name, phone number and avatar URL of the authenticated user."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Profile updated"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @PutMapping("/me/profile")
    public ResponseEntity<Void> updateProfile(@CurrentUser Long userId, @RequestBody UpdateProfileRequest request) {
        var command = new UpdateProfileCommand(userId, request.fullName(), request.phoneNumber(), request.avatarUrl());
        SpringValidation.validate(command, validator);
        updateProfileUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Change my password",
        description = "Changes the password of the authenticated user."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Password changed"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input, or the old password is wrong",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(@CurrentUser Long userId, @RequestBody ChangePasswordRequest request) {
        var command = new ChangePasswordCommand(userId, request.oldPassword(), request.newPassword());
        SpringValidation.validate(command, validator);
        changePasswordUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }
}

record UpdateProfileRequest(String fullName, String phoneNumber, String avatarUrl) {}

record ChangePasswordRequest(String oldPassword, String newPassword) {}
