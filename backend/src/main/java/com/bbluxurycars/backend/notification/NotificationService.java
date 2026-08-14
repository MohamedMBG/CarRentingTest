package com.bbluxurycars.backend.notification;

import com.bbluxurycars.backend.error.ApiException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Closes the {@code POST /v1/mobile/notifications/email} gap in
 * docs/SAAS_ROADMAP.md 1.3. Backs both {@code EmailSender} (an agency admin
 * notifying a renter about their booking) and any future server-triggered
 * mail -- the client never holds SMTP credentials.
 *
 * <p>{@link ObjectProvider} rather than a plain {@link JavaMailSender}
 * dependency: the bean only exists when {@code mail.host} is configured (see
 * {@link MailConfig}), and this lets the service start either way and fail
 * per-request instead of failing to boot.
 */
@Service
public class NotificationService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String fromAddress;

    public NotificationService(ObjectProvider<JavaMailSender> mailSenderProvider,
                               @Value("${mail.from:no-reply@bbluxurycars.com}") String fromAddress) {
        this.mailSenderProvider = mailSenderProvider;
        this.fromAddress = fromAddress;
    }

    public void sendEmail(String recipient, String subject, String body) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "notifications_not_configured",
                    "Outbound email is not configured on this instance.");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);

        try {
            sender.send(message);
        } catch (MailException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "notification_delivery_failed",
                    "Failed to send the notification email.");
        }
    }
}
