package dev.onion.aicoding.task;

import dev.onion.aicoding.review.ReviewRecord;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class TaskPlanner {

    private static final int MAX_TASKS_PER_REVIEW = 10;

    public TaskPlan createPlan(ReviewRecord review) {
        Set<String> candidates = new LinkedHashSet<>();
        collectLines(review.reviewText(), candidates);
        if (candidates.isEmpty()) {
            collectLines(review.suggestedCodexPrompt(), candidates);
        }
        List<TaskItem> tasks = new ArrayList<>();
        for (String candidate : candidates.stream().limit(MAX_TASKS_PER_REVIEW).toList()) {
            tasks.add(new TaskItem(UUID.randomUUID().toString(), title(candidate),
                    candidate, priority(candidate), TaskStatus.TODO,
                    review.changedFiles(), review.id(), Instant.now()));
        }
        if (tasks.isEmpty() && !review.suggestedCodexPrompt().isBlank()) {
            String description = truncate(review.suggestedCodexPrompt().strip(), 500);
            tasks.add(new TaskItem(UUID.randomUUID().toString(),
                    "Apply the suggested implementation", description,
                    TaskItem.Priority.MEDIUM, TaskStatus.TODO,
                    review.changedFiles(), review.id(), Instant.now()));
        }
        return new TaskPlan(tasks);
    }

    private void collectLines(String text, Set<String> candidates) {
        if (text == null) {
            return;
        }
        for (String rawLine : text.split("\\R")) {
            String line = rawLine.strip().replaceFirst("^[-*•\\d.)\\s]+", "").strip();
            String lower = line.toLowerCase(Locale.ROOT);
            if (line.length() >= 15 && !line.endsWith(":")
                    && !lower.startsWith("provider:")
                    && !lower.startsWith("elapsed:")
                    && (isBullet(rawLine) || isActionable(lower))) {
                candidates.add(truncate(line, 500));
            }
        }
    }

    private boolean isBullet(String line) {
        return line.stripLeading().matches("^([-*•]|\\d+[.)]).*");
    }

    private boolean isActionable(String line) {
        return List.of("add ", "fix ", "remove ", "update ", "change ", "refactor ",
                "implement ", "ensure ", "validate ", "test ", "avoid ", "consider ")
                .stream().anyMatch(line::startsWith);
    }

    private TaskItem.Priority priority(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.matches(".*\\b(security|critical|crash|data loss|vulnerability|bug)\\b.*")) {
            return TaskItem.Priority.HIGH;
        }
        if (lower.matches(".*\\b(test|refactor|validate|error|thread|concurrent)\\b.*")) {
            return TaskItem.Priority.MEDIUM;
        }
        return TaskItem.Priority.LOW;
    }

    private String title(String description) {
        String firstSentence = description.split("(?<=[.!?])\\s+", 2)[0];
        return truncate(firstSentence, 90);
    }

    private String truncate(String value, int maximum) {
        return value.length() <= maximum ? value : value.substring(0, maximum - 1) + "…";
    }
}
