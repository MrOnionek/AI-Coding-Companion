package dev.onion.aicoding.ai;

public interface AIProvider {

    String getName();

    boolean isAvailable();

    AIResponse send(AIRequest request);
}
