package dev.onion.aicoding.model;

public record ProjectInfo(
        String name,
        String absolutePath,
        boolean gitRepository,
        String buildSystem,
        String language) {
}
