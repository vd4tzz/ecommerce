package io.d4tzz.ecommerce.infrastructure.notification;

import io.d4tzz.ecommerce.domain.user.Email;
import java.util.Map;

public interface EmailSender {

    void send(Email to, String subject, String template, Map<String, Object> variables);
}
