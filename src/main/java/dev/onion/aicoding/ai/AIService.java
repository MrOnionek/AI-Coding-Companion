package dev.onion.aicoding.ai;

import dev.onion.aicoding.providers.ChatGPTProvider;
import dev.onion.aicoding.providers.ClaudeProvider;
import dev.onion.aicoding.providers.GeminiProvider;
import dev.onion.aicoding.providers.OllamaProvider;
import java.util.List;
import java.util.Optional;

public class AIService {

    private final List<AIProvider> providers;
    private AIProvider activeProvider;

    public AIService() {
        providers = List.of(new ChatGPTProvider(), new ClaudeProvider(),
                new GeminiProvider(), new OllamaProvider());
        activeProvider = providers.getFirst();
    }

    public List<AIProvider> getProviders() {
        return providers;
    }

    public AIProvider getActiveProvider() {
        return activeProvider;
    }

    public boolean setActiveProvider(String providerName) {
        Optional<AIProvider> selected = providers.stream()
                .filter(provider -> provider.getName().equals(providerName))
                .findFirst();
        selected.ifPresent(provider -> activeProvider = provider);
        return selected.isPresent();
    }

    public AIResponse send(AIRequest request) {
        return activeProvider.send(request);
    }
}
