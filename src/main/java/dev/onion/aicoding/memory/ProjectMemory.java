package dev.onion.aicoding.memory;

import java.util.List;

public record ProjectMemory(List<MemoryEntry> entries) {

    public ProjectMemory {
        entries = List.copyOf(entries);
    }

    public static ProjectMemory empty() {
        return new ProjectMemory(List.of());
    }

    public String toPromptText() {
        if (entries.isEmpty()) {
            return "(no project memory recorded)";
        }
        return entries.stream()
                .map(entry -> "- [" + entry.type() + "] " + entry.content())
                .collect(java.util.stream.Collectors.joining("\n"));
    }
}
