package io.d4tzz.ecommerce.user.infrastructure.notification.email;

import io.d4tzz.ecommerce.user.domain.Email;
import java.util.Map;

public interface EmailSender {

    void send(Email to, String subject, String template, Map<String, Object> variables);
}
