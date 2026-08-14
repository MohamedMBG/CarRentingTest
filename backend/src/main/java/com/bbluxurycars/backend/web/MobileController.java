package com.bbluxurycars.backend.web;

import com.bbluxurycars.backend.concierge.ConciergeService;
import com.bbluxurycars.backend.notification.NotificationService;
import com.bbluxurycars.backend.web.dto.ConciergeRequest;
import com.bbluxurycars.backend.web.dto.ConciergeResponse;
import com.bbluxurycars.backend.web.dto.EmailNotificationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The two server-side proxies the Android app already calls but that have
 * never existed on this side: {@code /v1/mobile/concierge} (Gemini, key kept
 * server-side) and {@code /v1/mobile/notifications/email} (SMTP, credentials
 * kept server-side). See docs/SAAS_ROADMAP.md 1.3.
 *
 * <p>Both routes sit behind {@code FirebaseAuthFilter} (see {@code WebConfig}),
 * so only a caller holding a valid Firebase ID token can drive either proxy --
 * neither the Gemini quota nor the SMTP relay is reachable anonymously.
 */
@RestController
public class MobileController {

    private final ConciergeService conciergeService;
    private final NotificationService notificationService;

    public MobileController(ConciergeService conciergeService, NotificationService notificationService) {
        this.conciergeService = conciergeService;
        this.notificationService = notificationService;
    }

    @PostMapping("/v1/mobile/concierge")
    public ConciergeResponse concierge(@Valid @RequestBody ConciergeRequest body) {
        return new ConciergeResponse(conciergeService.recommend(body.query(), body.inventoryContext()));
    }

    @PostMapping("/v1/mobile/notifications/email")
    @ResponseStatus(HttpStatus.OK)
    public void notifyByEmail(@Valid @RequestBody EmailNotificationRequest body) {
        notificationService.sendEmail(body.recipient(), body.subject(), body.body());
    }
}
