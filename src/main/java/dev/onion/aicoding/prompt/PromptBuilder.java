package dev.onion.aicoding.prompt;

import dev.onion.aicoding.ai.AIRequest;
import dev.onion.aicoding.project.ProjectAnalysis;
import dev.onion.aicoding.memory.ProjectMemory;

public interface PromptBuilder {

    AIRequest build(ProjectAnalysis analysis, ProjectMemory memory,
                    String gitDiff, String userPrompt);
}
