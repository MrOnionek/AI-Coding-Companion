package dev.onion.aicoding.task;

import java.time.Instant;
import java.util.List;

public record TaskItem(
        String id,
        String title,
        String description,
        Priority priority,
        TaskStatus status,
        List<String> affectedFiles,
        String sourceReviewId,
        Instant createdTimestamp) {

    public enum Priority {
        LOW,
        MEDIUM,
        HIGH
    }

    public TaskItem {
        affectedFiles = List.copyOf(affectedFiles);
    }

    public TaskItem withStatus(TaskStatus newStatus) {
        return new TaskItem(id, title, description, priority, newStatus,
                affectedFiles, sourceReviewId, createdTimestamp);
    }
}
