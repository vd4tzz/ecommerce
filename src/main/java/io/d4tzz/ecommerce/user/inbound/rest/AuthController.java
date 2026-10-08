package io.d4tzz.ecommerce.user.inbound.rest;

import io.d4tzz.ecommerce.spring.SpringValidation;
import io.d4tzz.ecommerce.user.application.LoginUseCase;
import io.d4tzz.ecommerce.user.application.RefreshTokenUseCase;
import io.d4tzz.ecommerce.user.application.RegisterUserUseCase;
import io.d4tzz.ecommerce.user.application.RequestPasswordResetUseCase;
import io.d4tzz.ecommerce.user.application.ResetPasswordUseCase;
import io.d4tzz.ecommerce.user.application.SendVerificationOtpUseCase;
import io.d4tzz.ecommerce.user.application.VerifyEmailUseCase;
import io.d4tzz.ecommerce.user.application.command.LoginCommand;
import io.d4tzz.ecommerce.user.application.command.RefreshTokenCommand;
import io.d4tzz.ecommerce.user.application.command.RegisterUserCommand;
import io.d4tzz.ecommerce.user.application.command.RequestPasswordResetCommand;
import io.d4tzz.ecommerce.user.application.command.ResetPasswordCommand;
import io.d4tzz.ecommerce.user.application.command.SendVerificationOtpCommand;
import io.d4tzz.ecommerce.user.application.command.VerifyEmailCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Endpoints for user authentication")
@SecurityRequirements
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final VerifyEmailUseCase verifyEmailUseCase;
    private final SendVerificationOtpUseCase sendVerificationOtpUseCase;
    private final RequestPasswordResetUseCase requestPasswordResetUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final Validator validator;

    public AuthController(
        RegisterUserUseCase registerUserUseCase,
        VerifyEmailUseCase verifyEmailUseCase,
        SendVerificationOtpUseCase sendVerificationOtpUseCase,
        RequestPasswordResetUseCase requestPasswordResetUseCase,
        ResetPasswordUseCase resetPasswordUseCase,
        LoginUseCase loginUseCase,
        RefreshTokenUseCase refreshTokenUseCase,
        Validator validator
    ) {
        this.registerUserUseCase = registerUserUseCase;
        this.verifyEmailUseCase = verifyEmailUseCase;
        this.sendVerificationOtpUseCase = sendVerificationOtpUseCase;
        this.requestPasswordResetUseCase = requestPasswordResetUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
        this.loginUseCase = loginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.validator = validator;
    }

    @Operation(
        summary = "Register an account",
        description = "Creates a customer account. The Location header points at the new user. The email still has to be verified with an OTP."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Account created"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Email already registered",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
    })
    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody RegisterUserRequest request) {
        var command = new RegisterUserCommand(request.email(), request.password(), request.fullName());
        SpringValidation.validate(command, validator);
        var userId = registerUserUseCase.execute(command);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/users/{id}")
            .buildAndExpand(userId.value())
            .toUri();
        return ResponseEntity.created(location).build();
    }

    @Operation(
        summary = "Log in",
        description = "Exchanges email and password for an access token and a refresh token."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token pair"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Wrong email or password",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Account is blocked",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
    })
    @PostMapping("/login")
    public ResponseEntity<LoginUseCase.Response> login(@RequestBody LoginRequest request) {
        var command = new LoginCommand(request.email(), request.password());
        SpringValidation.validate(command, validator);
        return ResponseEntity.ok(loginUseCase.execute(command));
    }

    @Operation(summary = "Refresh tokens", description = "Exchanges a refresh token for a new token pair.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "New token pair"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Refresh token is invalid or was already used",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
    })
    @PostMapping("/refresh-token")
    public ResponseEntity<RefreshTokenUseCase.Response> refreshToken(@RequestBody RefreshTokenRequest request) {
        var command = new RefreshTokenCommand(request.refreshToken());
        SpringValidation.validate(command, validator);
        return ResponseEntity.ok(refreshTokenUseCase.execute(command));
    }

    @Operation(
        summary = "Verify email",
        description = "Verifies the account email with the OTP sent by send-verification-otp."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Email verified"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input, or the code is wrong or expired",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
    })
    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestBody VerifyEmailRequest request) {
        var command = new VerifyEmailCommand(request.email(), request.code());
        SpringValidation.validate(command, validator);
        verifyEmailUseCase.execute(command);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Send email verification OTP", description = "Emails a verification OTP, valid for 5 minutes.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OTP sent"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "429",
            description = "OTP requested too often",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
    })
    @PostMapping("/send-verification-otp")
    public ResponseEntity<Void> sendVerificationOtp(@RequestBody SendVerificationOtpRequest request) {
        var command = new SendVerificationOtpCommand(request.email());
        SpringValidation.validate(command, validator);
        sendVerificationOtpUseCase.execute(command);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Request password reset", description = "Emails a password-reset OTP, valid for 3 minutes.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OTP sent"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "429",
            description = "OTP requested too often",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody RequestPasswordResetRequest request) {
        var command = new RequestPasswordResetCommand(request.email());
        SpringValidation.validate(command, validator);
        requestPasswordResetUseCase.execute(command);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Reset password", description = "Sets a new password using the OTP sent by forgot-password.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Password reset"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input, or the code is wrong or expired",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
    })
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        var command = new ResetPasswordCommand(request.email(), request.code(), request.newPassword());
        SpringValidation.validate(command, validator);
        resetPasswordUseCase.execute(command);
        return ResponseEntity.ok().build();
    }
}

record RegisterUserRequest(String email, String password, String fullName) {}

record LoginRequest(String email, String password) {}

record RefreshTokenRequest(String refreshToken) {}

record VerifyEmailRequest(String email, String code) {}

record SendVerificationOtpRequest(String email) {}

record RequestPasswordResetRequest(String email) {}

record ResetPasswordRequest(String email, String code, String newPassword) {}
