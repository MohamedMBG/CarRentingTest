package com.bbluxurycars.backend.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Registers the SMTP sender behind {@code POST /v1/mobile/notifications/email}
 * only when one is configured.
 *
 * <p>Conditional on {@code mail.host} rather than always present, mirroring
 * {@code FirebaseAdminConfig}: the service must still start locally and in CI
 * without SMTP credentials, with {@link NotificationService} reporting 503 on
 * that specific endpoint instead of the whole context failing to start.
 */
@Configuration
public class MailConfig {

    @Bean
    @ConditionalOnProperty(prefix = "mail", name = "host")
    public JavaMailSender javaMailSender(@Value("${mail.host}") String host,
                                         @Value("${mail.port:587}") int port,
                                         @Value("${mail.username:}") String username,
                                         @Value("${mail.password:}") String password) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        return sender;
    }
}
