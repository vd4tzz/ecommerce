package io.d4tzz.ecommerce.user.infrastructure.notification.listener;

import io.d4tzz.ecommerce.user.domain.event.UserRegisteredEvent;
import io.d4tzz.ecommerce.user.infrastructure.notification.email.EmailSender;
import java.util.Map;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class UserRegisteredNotificationListener {

    private final EmailSender emailSender;

    public UserRegisteredNotificationListener(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @ApplicationModuleListener
    public void onUserRegistered(UserRegisteredEvent event) {
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
