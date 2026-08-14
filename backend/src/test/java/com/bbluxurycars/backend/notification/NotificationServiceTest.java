package com.bbluxurycars.backend.notification;

import com.bbluxurycars.backend.error.ApiException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link NotificationService} against a hand-written {@link JavaMailSender}
 * and {@link ObjectProvider}: the bean is conditional (see {@link MailConfig}),
 * so the "not configured" path -- an empty provider -- is a real state to
 * cover, not an edge case.
 */
class NotificationServiceTest {

    @Test
    void refusesWithAStableCodeWhenNoMailSenderIsConfigured() {
        NotificationService service = new NotificationService(
                new SingleValueProvider(null), "no-reply@bbluxurycars.com");

        assertThatThrownBy(() -> service.sendEmail("renter@example.com", "Booking approved", "See you soon."))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(e.getCode()).isEqualTo("notifications_not_configured");
                });
    }

    @Test
    void sendsTheMessageThroughTheConfiguredSender() {
        FakeMailSender sender = new FakeMailSender(false);
        NotificationService service = new NotificationService(
                new SingleValueProvider(sender), "no-reply@bbluxurycars.com");

        service.sendEmail("renter@example.com", "Booking approved", "See you soon.");

        assertThat(sender.lastMessage).isNotNull();
        assertThat(sender.lastMessage.getTo()).containsExactly("renter@example.com");
        assertThat(sender.lastMessage.getFrom()).isEqualTo("no-reply@bbluxurycars.com");
        assertThat(sender.lastMessage.getSubject()).isEqualTo("Booking approved");
    }

    @Test
    void turnsADeliveryFailureIntoAStableUpstreamErrorCode() {
        NotificationService service = new NotificationService(
                new SingleValueProvider(new FakeMailSender(true)), "no-reply@bbluxurycars.com");

        assertThatThrownBy(() -> service.sendEmail("renter@example.com", "Booking approved", "See you soon."))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(e.getCode()).isEqualTo("notification_delivery_failed");
                });
    }

    private record SingleValueProvider(JavaMailSender value) implements ObjectProvider<JavaMailSender> {
        @Override
        public JavaMailSender getObject() {
            return value;
        }

        @Override
        public JavaMailSender getObject(Object... args) {
            return value;
        }

        @Override
        public JavaMailSender getIfAvailable() {
            return value;
        }

        @Override
        public JavaMailSender getIfUnique() {
            return value;
        }
    }

    private static final class FakeMailSender implements JavaMailSender {
        private final boolean shouldFail;
        private SimpleMailMessage lastMessage;

        FakeMailSender(boolean shouldFail) {
            this.shouldFail = shouldFail;
        }

        @Override
        public void send(SimpleMailMessage simpleMessage) throws MailException {
            if (shouldFail) {
                throw new MailSendException("Simulated SMTP failure");
            }
            this.lastMessage = simpleMessage;
        }

        @Override
        public void send(SimpleMailMessage... simpleMessages) throws MailException {
            for (SimpleMailMessage message : simpleMessages) {
                send(message);
            }
        }

        @Override
        public MimeMessage createMimeMessage() {
            throw new UnsupportedOperationException("not exercised by NotificationService");
        }

        @Override
        public MimeMessage createMimeMessage(InputStream contentStream) {
            throw new UnsupportedOperationException("not exercised by NotificationService");
        }

        @Override
        public void send(MimeMessage mimeMessage) throws MailException {
            throw new UnsupportedOperationException("not exercised by NotificationService");
        }

        @Override
        public void send(MimeMessage... mimeMessages) throws MailException {
            throw new UnsupportedOperationException("not exercised by NotificationService");
        }

        @Override
        public void send(MimeMessagePreparator mimeMessagePreparator) throws MailException {
            throw new UnsupportedOperationException("not exercised by NotificationService");
        }

        @Override
        public void send(MimeMessagePreparator... mimeMessagePreparators) throws MailException {
            throw new UnsupportedOperationException("not exercised by NotificationService");
        }
    }
}
