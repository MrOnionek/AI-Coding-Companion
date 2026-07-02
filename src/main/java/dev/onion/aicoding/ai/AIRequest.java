package dev.onion.aicoding.ai;

public record AIRequest(
        String systemPrompt,
        String userPrompt,
        String projectSummary,
        String gitDiff) {

    public AIRequest {
        systemPrompt = valueOrEmpty(systemPrompt);
        userPrompt = valueOrEmpty(userPrompt);
        projectSummary = valueOrEmpty(projectSummary);
        gitDiff = valueOrEmpty(gitDiff);
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
