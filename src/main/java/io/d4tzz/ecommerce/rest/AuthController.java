package io.d4tzz.ecommerce.rest;

import io.d4tzz.ecommerce.application.user.LoginUseCase;
import io.d4tzz.ecommerce.application.user.RefreshTokenUseCase;
import io.d4tzz.ecommerce.application.user.RegisterUserUseCase;
import io.d4tzz.ecommerce.application.user.RequestPasswordResetUseCase;
import io.d4tzz.ecommerce.application.user.ResetPasswordUseCase;
import io.d4tzz.ecommerce.application.user.SendVerificationOtpUseCase;
import io.d4tzz.ecommerce.application.user.UserDto;
import io.d4tzz.ecommerce.application.user.VerifyEmailUseCase;
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
    public ResponseEntity<UserDto> register(@RequestBody RegisterUserRequest request) {
        var command = new RegisterUserUseCase.Command(request.email(), request.password(), request.fullName());
        UserDto response = registerUserUseCase.execute(command);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/users/{id}")
            .buildAndExpand(response.id())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginUseCase.Response> login(@RequestBody LoginRequest request) {
        var command = new LoginUseCase.Command(request.email(), request.password());
        return ResponseEntity.ok(loginUseCase.execute(command));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<RefreshTokenUseCase.Response> refreshToken(@RequestBody RefreshTokenRequest request) {
        var command = new RefreshTokenUseCase.Command(request.refreshToken());
        return ResponseEntity.ok(refreshTokenUseCase.execute(command));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestBody VerifyEmailRequest request) {
        var command = new VerifyEmailUseCase.Command(request.email(), request.code());
        verifyEmailUseCase.execute(command);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/send-verification-otp")
    public ResponseEntity<Void> sendVerificationOtp(@RequestBody SendVerificationOtpRequest request) {
        var command = new SendVerificationOtpUseCase.Command(request.email());
        sendVerificationOtpUseCase.execute(command);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody RequestPasswordResetRequest request) {
        var command = new RequestPasswordResetUseCase.Command(request.email());
        requestPasswordResetUseCase.execute(command);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        var command = new ResetPasswordUseCase.Command(request.email(), request.code(), request.newPassword());
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
