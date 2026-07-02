package dev.onion.aicoding.task;

import java.util.List;

public record TaskPlan(List<TaskItem> tasks) {

    public TaskPlan {
        tasks = List.copyOf(tasks);
    }

    public static TaskPlan empty() {
        return new TaskPlan(List.of());
    }

    public List<TaskItem> openTasks() {
        return tasks.stream().filter(task -> task.status() == TaskStatus.TODO
                || task.status() == TaskStatus.IN_PROGRESS).toList();
    }

    public String toPromptText() {
        List<TaskItem> open = openTasks();
        return open.isEmpty() ? "(no open tasks)" : open.stream()
                .map(task -> "- [" + task.priority() + "] " + task.title()
                        + ": " + task.description())
                .collect(java.util.stream.Collectors.joining("\n"));
    }
}
