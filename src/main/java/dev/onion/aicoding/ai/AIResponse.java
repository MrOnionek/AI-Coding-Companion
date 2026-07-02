package dev.onion.aicoding.ai;

import java.time.Duration;
import java.util.OptionalLong;

public record AIResponse(
        String responseText,
        Duration elapsedTime,
        String providerName,
        OptionalLong tokenUsage) {

    public AIResponse {
        responseText = responseText == null ? "" : responseText;
        elapsedTime = elapsedTime == null ? Duration.ZERO : elapsedTime;
        providerName = providerName == null ? "" : providerName;
        tokenUsage = tokenUsage == null ? OptionalLong.empty() : tokenUsage;
    }
}
