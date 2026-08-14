package com.bbluxurycars.backend.concierge;

import com.bbluxurycars.backend.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Closes the {@code POST /v1/mobile/concierge} gap in
 * docs/SAAS_ROADMAP.md 1.3: the Android app already calls this endpoint and
 * gets nothing back today.
 */
@Service
public class ConciergeService {

    private final GeminiClient geminiClient;

    public ConciergeService(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    public String recommend(String query, String inventoryContext) {
        if (!geminiClient.isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "concierge_not_configured",
                    "The concierge assistant is not configured on this instance.");
        }
        String recommendation = geminiClient.generateText(buildPrompt(query, inventoryContext));
        if (recommendation == null || recommendation.isBlank()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "concierge_upstream_failed",
                    "The concierge assistant did not return a recommendation.");
        }
        return recommendation;
    }

    private String buildPrompt(String query, String inventoryContext) {
        return """
                You are the in-app rental concierge for a car rental agency. \
                Recommend a vehicle from the inventory below that best matches \
                the renter's request. Be concise, speak directly to the \
                renter, and only recommend vehicles that appear in the \
                inventory.

                Renter request: %s

                Available inventory:
                %s
                """.formatted(query, inventoryContext == null || inventoryContext.isBlank()
                ? "(no inventory context provided)" : inventoryContext);
    }
}
