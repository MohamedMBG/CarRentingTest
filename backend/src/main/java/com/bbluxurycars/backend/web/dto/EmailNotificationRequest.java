package com.bbluxurycars.backend.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Body of {@code POST /v1/mobile/notifications/email}, matching {@code EmailSender} on the client. */
public record EmailNotificationRequest(
        @NotBlank @Email String recipient,
        @NotBlank String subject,
        @NotBlank String body) {
}
