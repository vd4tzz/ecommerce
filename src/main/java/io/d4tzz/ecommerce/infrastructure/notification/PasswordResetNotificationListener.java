package io.d4tzz.ecommerce.infrastructure.notification;

import io.d4tzz.ecommerce.domain.user.event.PasswordResetRequestedEvent;
import java.util.Map;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetNotificationListener {

    private final EmailSender emailSender;

    public PasswordResetNotificationListener(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @ApplicationModuleListener
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        emailSender.send(
            event.email(),
            "Password Reset Request",
            "mail/otp-verification",
            Map.of(
                "title", "Reset Your Password",
                "message", "We received a request to reset your password. Please use the verification code below:",
                "otpCode", event.otpCode().value(),
                "validityMinutes", 5
            )
        );
    }
}
