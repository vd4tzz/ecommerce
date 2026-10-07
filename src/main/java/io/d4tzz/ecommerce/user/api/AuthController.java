package io.d4tzz.ecommerce.user.api;

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
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Endpoints for user authentication")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final VerifyEmailUseCase verifyEmailUseCase;
    private final SendVerificationOtpUseCase sendVerificationOtpUseCase;
    private final RequestPasswordResetUseCase requestPasswordResetUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;

    public AuthController(
        RegisterUserUseCase registerUserUseCase,
        VerifyEmailUseCase verifyEmailUseCase,
        SendVerificationOtpUseCase sendVerificationOtpUseCase,
        RequestPasswordResetUseCase requestPasswordResetUseCase,
        ResetPasswordUseCase resetPasswordUseCase,
        LoginUseCase loginUseCase,
        RefreshTokenUseCase refreshTokenUseCase
    ) {
        this.registerUserUseCase = registerUserUseCase;
        this.verifyEmailUseCase = verifyEmailUseCase;
        this.sendVerificationOtpUseCase = sendVerificationOtpUseCase;
        this.requestPasswordResetUseCase = requestPasswordResetUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
        this.loginUseCase = loginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody RegisterUserRequest request) {
        var command = new RegisterUserCommand(request.email(), request.password(), request.fullName());
        var userId = registerUserUseCase.execute(command);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/users/{id}")
            .buildAndExpand(userId.value())
            .toUri();
        return ResponseEntity.created(location).build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginUseCase.Response> login(@RequestBody LoginRequest request) {
        var command = new LoginCommand(request.email(), request.password());
        return ResponseEntity.ok(loginUseCase.execute(command));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<RefreshTokenUseCase.Response> refreshToken(@RequestBody RefreshTokenRequest request) {
        var command = new RefreshTokenCommand(request.refreshToken());
        return ResponseEntity.ok(refreshTokenUseCase.execute(command));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestBody VerifyEmailRequest request) {
        var command = new VerifyEmailCommand(request.email(), request.code());
        verifyEmailUseCase.execute(command);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/send-verification-otp")
    public ResponseEntity<Void> sendVerificationOtp(@RequestBody SendVerificationOtpRequest request) {
        var command = new SendVerificationOtpCommand(request.email());
        sendVerificationOtpUseCase.execute(command);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody RequestPasswordResetRequest request) {
        var command = new RequestPasswordResetCommand(request.email());
        requestPasswordResetUseCase.execute(command);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        var command = new ResetPasswordCommand(request.email(), request.code(), request.newPassword());
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
