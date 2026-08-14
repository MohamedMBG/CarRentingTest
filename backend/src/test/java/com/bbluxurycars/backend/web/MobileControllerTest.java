package com.bbluxurycars.backend.web;

import com.bbluxurycars.backend.concierge.ConciergeService;
import com.bbluxurycars.backend.concierge.GeminiClient;
import com.bbluxurycars.backend.notification.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The wire contract of the two proxy endpoints, wired to real service
 * instances over a stub {@link GeminiClient} and an absent
 * {@link JavaMailSender} -- no Spring context or database needed, since
 * neither controller touches persistence.
 */
class MobileControllerTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new MobileController(
                    new ConciergeService(new StubGeminiClient()),
                    new NotificationService(new EmptyMailSenderProvider(), "no-reply@bbluxurycars.com")))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    @Test
    void returnsTheConciergesRecommendation() throws Exception {
        mockMvc.perform(post("/v1/mobile/concierge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"A cheap SUV for the weekend",
                                 "inventoryContext":"Duster, Clio"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendation").value("Take the Duster."));
    }

    @Test
    void rejectsAConciergeRequestWithNoQuery() throws Exception {
        mockMvc.perform(post("/v1/mobile/concierge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"inventoryContext":"Duster, Clio"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_request"));
    }

    @Test
    void reportsEmailNotConfiguredWithAStableCodeRatherThanA500() throws Exception {
        mockMvc.perform(post("/v1/mobile/notifications/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipient":"renter@example.com",
                                 "subject":"Booking approved",
                                 "body":"See you soon."}
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("notifications_not_configured"));
    }

    @Test
    void rejectsANotificationRequestWithAMalformedRecipient() throws Exception {
        mockMvc.perform(post("/v1/mobile/notifications/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipient":"not-an-email",
                                 "subject":"Booking approved",
                                 "body":"See you soon."}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_request"));
    }

    private static final class StubGeminiClient implements GeminiClient {
        @Override
        public boolean isConfigured() {
            return true;
        }

        @Override
        public String generateText(String prompt) {
            return "Take the Duster.";
        }
    }

    private record EmptyMailSenderProvider() implements ObjectProvider<JavaMailSender> {
        @Override
        public JavaMailSender getObject() {
            return null;
        }

        @Override
        public JavaMailSender getObject(Object... args) {
            return null;
        }

        @Override
        public JavaMailSender getIfAvailable() {
            return null;
        }

        @Override
        public JavaMailSender getIfUnique() {
            return null;
        }
    }
}
