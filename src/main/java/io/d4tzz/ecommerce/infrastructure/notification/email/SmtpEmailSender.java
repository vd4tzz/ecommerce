package io.d4tzz.ecommerce.infrastructure.notification.email;

import io.d4tzz.ecommerce.domain.user.Email;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public SmtpEmailSender(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Override
    public void send(Email to, String subject, String template, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        context.setVariable("subject", subject);

        String htmlContent = templateEngine.process(template, context);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(to.value());
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email to " + to.value(), e);
        }
    }
}
