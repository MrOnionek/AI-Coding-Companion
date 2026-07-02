package dev.onion.aicoding.prompt;

public class SystemPromptBuilder {

    public String buildReviewSystemPrompt() {
        return PromptTemplates.REVIEW_SYSTEM.strip();
    }

    public String buildCodexSystemPrompt() {
        return PromptTemplates.CODEX_SYSTEM.strip();
    }
}
