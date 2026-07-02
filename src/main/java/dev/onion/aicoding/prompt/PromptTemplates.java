package dev.onion.aicoding.prompt;

public final class PromptTemplates {

    public static final String REVIEW_SYSTEM = """
            You are a senior Java code reviewer. Give a concise, actionable review.
            Prioritize correctness, security, maintainability, and test coverage.
            """;

    public static final String CODEX_SYSTEM = """
            You are a senior Java implementation assistant.
            Produce a concrete, minimal implementation plan suitable for a coding agent.
            Preserve the existing architecture and avoid unrelated changes.
            """;

    public static final String REVIEW_REQUEST = """
            Review the following project changes.

            Project Analysis:
            %s

            Project Memory:
            %s

            Open Tasks:
            %s

            Git Diff:
            %s

            Review Request:
            %s
            """;

    public static final String IMPLEMENTATION_REQUEST = """
            Prepare an implementation prompt for this project.

            Project Analysis:
            %s

            Project Memory:
            %s

            Open Tasks:
            %s

            Current Git Diff:
            %s

            Requested Change:
            %s
            """;

    private PromptTemplates() {
    }
}
