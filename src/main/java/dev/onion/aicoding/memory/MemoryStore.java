package dev.onion.aicoding.memory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MemoryStore {

    private static final Pattern ENTRY = Pattern.compile(
            "\\{\"type\":\"([A-Z_]+)\",\"content\":\"((?:\\\\.|[^\"])*)\","
                    + "\"createdAt\":\"((?:\\\\.|[^\"])*)\"}");

    public ProjectMemory load(Path projectPath) throws IOException {
        Path file = memoryFile(projectPath);
        if (!Files.isRegularFile(file)) {
            return ProjectMemory.empty();
        }
        String json = Files.readString(file, StandardCharsets.UTF_8);
        List<MemoryEntry> entries = new ArrayList<>();
        Matcher matcher = ENTRY.matcher(json);
        while (matcher.find()) {
            try {
                entries.add(new MemoryEntry(
                        MemoryEntry.Type.valueOf(matcher.group(1)),
                        unescape(matcher.group(2)),
                        Instant.parse(unescape(matcher.group(3)))));
            } catch (IllegalArgumentException ignored) {
                // Skip malformed entries while preserving valid project memory.
            }
        }
        return new ProjectMemory(entries);
    }

    public void save(Path projectPath, ProjectMemory memory) throws IOException {
        Path file = memoryFile(projectPath);
        Files.createDirectories(file.getParent());
        String entries = memory.entries().stream().map(entry -> """
                {"type":"%s","content":"%s","createdAt":"%s"}"""
                .formatted(entry.type(), escape(entry.content()), entry.createdAt()))
                .collect(java.util.stream.Collectors.joining(","));
        Files.writeString(file, "{\"entries\":[" + entries + "]}",
                StandardCharsets.UTF_8);
    }

    private Path memoryFile(Path projectPath) {
        return projectPath.resolve(".ai-coding-companion").resolve("memory.json");
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private String unescape(String value) {
        StringBuilder result = new StringBuilder();
        boolean escaped = false;
        for (char character : value.toCharArray()) {
            if (escaped) {
                result.append(switch (character) {
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    default -> character;
                });
                escaped = false;
            } else if (character == '\\') {
                escaped = true;
            } else {
                result.append(character);
            }
        }
        return result.toString();
    }
}
