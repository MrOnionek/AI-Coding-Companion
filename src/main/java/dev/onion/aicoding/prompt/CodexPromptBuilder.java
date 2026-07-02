package dev.onion.aicoding.prompt;

import dev.onion.aicoding.ai.AIRequest;
import dev.onion.aicoding.project.ProjectAnalysis;
import dev.onion.aicoding.memory.ProjectMemory;

public class CodexPromptBuilder implements PromptBuilder {

    private final SystemPromptBuilder systemPromptBuilder;

    public CodexPromptBuilder() {
        this(new SystemPromptBuilder());
    }

    public CodexPromptBuilder(SystemPromptBuilder systemPromptBuilder) {
        this.systemPromptBuilder = systemPromptBuilder;
    }

    @Override
    public AIRequest build(ProjectAnalysis analysis, ProjectMemory memory,
                           String gitDiff, String userPrompt) {
        String projectSummary = ReviewPromptBuilder.formatAnalysis(analysis);
        String fullPrompt = PromptTemplates.IMPLEMENTATION_REQUEST.formatted(
                projectSummary, memory.toPromptText(),
                gitDiff == null || gitDiff.isBlank() ? "(no changes)" : gitDiff,
                userPrompt == null || userPrompt.isBlank()
                        ? "Suggest the next implementation step." : userPrompt);
        return new AIRequest(systemPromptBuilder.buildCodexSystemPrompt(),
                fullPrompt, projectSummary, gitDiff);
    }
}
