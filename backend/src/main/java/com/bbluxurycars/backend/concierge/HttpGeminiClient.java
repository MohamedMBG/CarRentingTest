package com.bbluxurycars.backend.concierge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * {@link GeminiClient} over Gemini's REST API, called directly rather than
 * through a Google SDK: the backend needs exactly one call shape, and a
 * dependency-free {@link HttpClient} keeps the concierge from dragging in a
 * whole client library for it.
 *
 * <p>The API key lives only here, read from server-side configuration
 * ({@code gemini.api-key}) -- the reason this proxy exists at all is that the
 * Android app must never hold it.
 */
@Component
public class HttpGeminiClient implements GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(HttpGeminiClient.class);
    private static final String ENDPOINT_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    private final String apiKey;
    private final String model;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HttpGeminiClient(@Value("${gemini.api-key:}") String apiKey,
                            @Value("${gemini.model:gemini-2.0-flash}") String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String generateText(String prompt) {
        if (!isConfigured()) {
            return null;
        }
        try {
            ObjectNode part = objectMapper.createObjectNode().put("text", prompt);
            ObjectNode content = objectMapper.createObjectNode();
            content.putArray("parts").add(part);
            ObjectNode body = objectMapper.createObjectNode();
            body.putArray("contents").add(content);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT_TEMPLATE.formatted(model, apiKey)))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(
                            objectMapper.writeValueAsBytes(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                log.warn("Gemini call failed with status {}", response.statusCode());
                return null;
            }
            return extractText(response.body());
        } catch (IOException e) {
            log.warn("Gemini call failed: {}", e.toString());
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Gemini call interrupted");
            return null;
        }
    }

    private String extractText(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode text = root.path("candidates").path(0)
                    .path("content").path("parts").path(0).path("text");
            return text.isMissingNode() ? null : text.asText();
        } catch (IOException e) {
            log.warn("Could not parse Gemini response: {}", e.toString());
            return null;
        }
    }
}
