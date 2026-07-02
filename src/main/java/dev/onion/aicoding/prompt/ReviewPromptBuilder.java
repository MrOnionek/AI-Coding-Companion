package dev.onion.aicoding.prompt;

import dev.onion.aicoding.ai.AIRequest;
import dev.onion.aicoding.project.ProjectAnalysis;
import dev.onion.aicoding.memory.ProjectMemory;
import dev.onion.aicoding.task.TaskPlan;

public class ReviewPromptBuilder implements PromptBuilder {

    private final SystemPromptBuilder systemPromptBuilder;

    public ReviewPromptBuilder() {
        this(new SystemPromptBuilder());
    }

    public ReviewPromptBuilder(SystemPromptBuilder systemPromptBuilder) {
        this.systemPromptBuilder = systemPromptBuilder;
    }

    @Override
    public AIRequest build(ProjectAnalysis analysis, ProjectMemory memory, TaskPlan tasks,
                           String gitDiff, String userPrompt) {
        String projectSummary = formatAnalysis(analysis);
        String fullPrompt = PromptTemplates.REVIEW_REQUEST.formatted(
                projectSummary, memory.toPromptText(), tasks.toPromptText(),
                valueOrPlaceholder(gitDiff, "(no changes)"),
                valueOrPlaceholder(userPrompt, "Provide a general code review."));
        return new AIRequest(systemPromptBuilder.buildReviewSystemPrompt(),
                fullPrompt, projectSummary, gitDiff);
    }

    static String formatAnalysis(ProjectAnalysis analysis) {
        var summary = analysis.summary();
        var info = summary.projectInfo();
        return """
                Project: %s
                Path: %s
                Git repository: %s
                Build system: %s
                Technologies: %s
                Java files: %d
                Packages: %s
                Declarations: %s
                Entry points: %s
                Important classes: %s
                Largest files: %s
                """.formatted(info.name(), info.absolutePath(), info.gitRepository(),
                info.buildSystem(), analysis.detectedTechnologies(),
                summary.totalJavaFiles(), summary.packageNames(),
                analysis.declarationCounts(), analysis.entryPoints(),
                analysis.importantClasses(), analysis.largestJavaFiles()).strip();
    }

    private String valueOrPlaceholder(String value, String placeholder) {
        return value == null || value.isBlank() ? placeholder : value;
    }
}
