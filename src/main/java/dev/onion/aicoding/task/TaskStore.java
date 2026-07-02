package dev.onion.aicoding.task;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class TaskStore {

    private final List<TaskItem> tasks = new ArrayList<>();
    private Path projectPath;
    private Consumer<TaskPlan> onTasksChanged = plan -> { };

    public synchronized void openProject(Path path) {
        projectPath = path.toAbsolutePath().normalize();
        tasks.clear();
        Path directory = taskDirectory();
        if (Files.isDirectory(directory)) {
            try (Stream<Path> files = Files.list(directory)) {
                for (Path file : files.filter(value -> value.toString().endsWith(".json"))
                        .toList()) {
                    try {
                        tasks.add(fromJson(Files.readString(file, StandardCharsets.UTF_8)));
                    } catch (IOException | RuntimeException ignored) {
                        // Preserve valid tasks if one file is unreadable or damaged.
                    }
                }
            } catch (IOException ignored) {
                // Tasks are optional and must not prevent project opening.
            }
        }
        notifyChanged();
    }

    public synchronized void save(TaskPlan plan) {
        if (projectPath == null || plan.tasks().isEmpty()) {
            return;
        }
        try {
            Files.createDirectories(taskDirectory());
            for (TaskItem task : plan.tasks()) {
                Files.writeString(taskFile(task.id()), toJson(task), StandardCharsets.UTF_8);
                tasks.removeIf(existing -> existing.id().equals(task.id()));
                tasks.add(task);
            }
            notifyChanged();
        } catch (IOException ignored) {
            // A review remains usable when task persistence is unavailable.
        }
    }

    public synchronized void updateStatus(String taskId, TaskStatus status) {
        for (int index = 0; index < tasks.size(); index++) {
            TaskItem task = tasks.get(index);
            if (task.id().equals(taskId)) {
                TaskItem updated = task.withStatus(status);
                tasks.set(index, updated);
                try {
                    Files.writeString(taskFile(taskId), toJson(updated),
                            StandardCharsets.UTF_8);
                } catch (IOException ignored) {
                    // Keep the status available in memory.
                }
                notifyChanged();
                return;
            }
        }
    }

    public synchronized TaskPlan currentPlan() {
        return new TaskPlan(tasks.stream()
                .sorted(Comparator.comparing(TaskItem::createdTimestamp).reversed())
                .toList());
    }

    public synchronized TaskPlan openTaskPlan() {
        return new TaskPlan(currentPlan().openTasks());
    }

    public void onTasksChanged(Consumer<TaskPlan> callback) {
        onTasksChanged = callback;
    }

    private void notifyChanged() {
        onTasksChanged.accept(currentPlan());
    }

    private Path taskDirectory() {
        return projectPath.resolve(".ai-coding-companion").resolve("tasks");
    }

    private Path taskFile(String id) {
        return taskDirectory().resolve(id + ".json");
    }

    private String toJson(TaskItem task) {
        String files = task.affectedFiles().stream()
                .map(file -> "\"" + escape(file) + "\"")
                .collect(java.util.stream.Collectors.joining(","));
        return """
                {"id":"%s","title":"%s","description":"%s","priority":"%s","status":"%s","affectedFiles":[%s],"sourceReviewId":"%s","createdTimestamp":"%s"}\
                """.formatted(escape(task.id()), escape(task.title()),
                escape(task.description()), task.priority(), task.status(), files,
                escape(task.sourceReviewId()), task.createdTimestamp());
    }

    private TaskItem fromJson(String json) {
        return new TaskItem(string(json, "id"), string(json, "title"),
                string(json, "description"),
                TaskItem.Priority.valueOf(string(json, "priority")),
                TaskStatus.valueOf(string(json, "status")),
                stringArray(json, "affectedFiles"),
                string(json, "sourceReviewId"),
                Instant.parse(string(json, "createdTimestamp")));
    }

    private String string(String json, String key) {
        int keyStart = json.indexOf("\"" + key + "\"");
        int colon = json.indexOf(':', keyStart);
        int quote = json.indexOf('"', colon);
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int index = quote + 1; index < json.length(); index++) {
            char character = json.charAt(index);
            if (escaped) {
                value.append(switch (character) {
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    default -> character;
                });
                escaped = false;
            } else if (character == '\\') {
                escaped = true;
            } else if (character == '"') {
                return value.toString();
            } else {
                value.append(character);
            }
        }
        throw new IllegalArgumentException("Invalid JSON string: " + key);
    }

    private List<String> stringArray(String json, String key) {
        int start = json.indexOf('[', json.indexOf("\"" + key + "\""));
        int end = json.indexOf(']', start);
        List<String> values = new ArrayList<>();
        int position = start + 1;
        while (position < end) {
            int quote = json.indexOf('"', position);
            if (quote < 0 || quote >= end) {
                break;
            }
            values.add(string("{\"value\":" + json.substring(quote, end) + "}", "value"));
            boolean escaped = false;
            int closing = quote + 1;
            for (; closing < end; closing++) {
                char character = json.charAt(closing);
                if (character == '"' && !escaped) {
                    break;
                }
                escaped = character == '\\' && !escaped;
                if (character != '\\') {
                    escaped = false;
                }
            }
            position = closing + 1;
        }
        return values;
    }

    private String escape(String value) {
        return (value == null ? "" : value).replace("\\", "\\\\")
                .replace("\"", "\\\"").replace("\n", "\\n")
                .replace("\r", "\\r").replace("\t", "\\t");
    }
}
