package io.d4tzz.ecommerce.infrastructure.notification;

import io.d4tzz.ecommerce.domain.user.event.EmailVerificationRequestedEvent;
import java.util.Map;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class EmailVerificationNotificationListener {

    private final EmailSender emailSender;

    public EmailVerificationNotificationListener(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @ApplicationModuleListener
    public void onEmailVerificationRequested(EmailVerificationRequestedEvent event) {
        emailSender.send(
            event.email(),
            "Verify your email",
            "mail/otp-verification",
            Map.of(
                "title", "Verify Your Email Address",
                "message", "Thank you for registering. Please use the verification code below to verify your email address:",
                "otpCode", event.otpCode().value(),
                "validityMinutes", 5
            )
        );
    }
}
