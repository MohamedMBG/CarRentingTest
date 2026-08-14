package com.bbluxurycars.backend.concierge;

import com.bbluxurycars.backend.error.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link ConciergeService} against a hand-written {@link GeminiClient} rather
 * than a mock: the point under test is how the service reacts to "not
 * configured" and "returned nothing", which a stub expresses more plainly
 * than call verification.
 */
class ConciergeServiceTest {

    @Test
    void refusesWithAStableCodeWhenNoApiKeyIsConfigured() {
        ConciergeService service = new ConciergeService(new StubGeminiClient(false, "unused"));

        assertThatThrownBy(() -> service.recommend("A cheap SUV for the weekend", "Duster, Clio"))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(e.getCode()).isEqualTo("concierge_not_configured");
                });
    }

    @Test
    void returnsTheModelsRecommendationWhenConfigured() {
        ConciergeService service = new ConciergeService(
                new StubGeminiClient(true, "The Dacia Duster suits a weekend trip well."));

        String recommendation = service.recommend("A cheap SUV for the weekend", "Duster, Clio");

        assertThat(recommendation).isEqualTo("The Dacia Duster suits a weekend trip well.");
    }

    @Test
    void treatsAnEmptyUpstreamResponseAsAFailureRatherThanAnEmptyRecommendation() {
        ConciergeService service = new ConciergeService(new StubGeminiClient(true, ""));

        assertThatThrownBy(() -> service.recommend("A cheap SUV", "Duster"))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(e.getCode()).isEqualTo("concierge_upstream_failed");
                });
    }

    private record StubGeminiClient(boolean configured, String response) implements GeminiClient {
        @Override
        public boolean isConfigured() {
            return configured;
        }

        @Override
        public String generateText(String prompt) {
            return response;
        }
    }
}
