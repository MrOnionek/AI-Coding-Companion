package dev.onion.aicoding.memory;

import java.time.Instant;

public record MemoryEntry(Type type, String content, Instant createdAt) {

    public enum Type {
        ARCHITECTURE_DECISION,
        COMPLETED_MILESTONE,
        KNOWN_BUG,
        TODO,
        CODING_CONVENTION,
        REVIEW_NOTE
    }

    public MemoryEntry {
        content = content == null ? "" : content;
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }
}
