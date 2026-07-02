package dev.onion.aicoding.providers;

import dev.onion.aicoding.ai.AIProvider;
import dev.onion.aicoding.ai.AIRequest;
import dev.onion.aicoding.ai.AIResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.OptionalLong;

public class GeminiProvider implements AIProvider {
    public String getName() { return "Gemini"; }
    public boolean isAvailable() { return true; }
    public AIResponse send(AIRequest request) {
        Instant started = Instant.now();
        return new AIResponse("Mock Gemini response. API connection is not configured.",
                Duration.between(started, Instant.now()), getName(), OptionalLong.empty());
    }
}
