package dev.onion.aicoding.prompt;

import dev.onion.aicoding.ai.AIRequest;
import dev.onion.aicoding.project.ProjectAnalysis;
import dev.onion.aicoding.memory.ProjectMemory;
import dev.onion.aicoding.task.TaskPlan;

public interface PromptBuilder {

    AIRequest build(ProjectAnalysis analysis, ProjectMemory memory, TaskPlan tasks,
                    String gitDiff, String userPrompt);
}
