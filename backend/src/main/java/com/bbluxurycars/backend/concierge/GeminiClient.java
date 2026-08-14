package com.bbluxurycars.backend.concierge;

/**
 * The narrow slice of Gemini the concierge needs: turn one prompt into one
 * answer.
 *
 * <p>An interface rather than calling the HTTP client directly so
 * {@link ConciergeService} is testable without a real API key or a network
 * call.
 */
public interface GeminiClient {

    /** Whether an API key is configured at all. */
    boolean isConfigured();

    /**
     * @return the model's text response, or {@code null}/blank if the upstream
     * call failed. Never throws for an ordinary upstream failure -- the caller
     * turns that into a stable API error instead of a stack trace.
     */
    String generateText(String prompt);
}
