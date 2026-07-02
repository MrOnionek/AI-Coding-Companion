package dev.onion.aicoding.ai;

import dev.onion.aicoding.providers.ChatGPTProvider;
import dev.onion.aicoding.providers.ClaudeProvider;
import dev.onion.aicoding.providers.GeminiProvider;
import dev.onion.aicoding.providers.OllamaProvider;
import java.util.List;
import java.util.Optional;
import dev.onion.aicoding.settings.Settings;

public class AIService {

    private final List<AIProvider> providers;
    private AIProvider activeProvider;

    public AIService(Settings settings) {
        providers = List.of(new ChatGPTProvider(settings::resolveOpenAIApiKey,
                settings::getReviewTimeoutSeconds), new ClaudeProvider(),
                new GeminiProvider(), new OllamaProvider());
        activeProvider = providers.getFirst();
        setActiveProvider(settings.getDefaultAIProvider());
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
