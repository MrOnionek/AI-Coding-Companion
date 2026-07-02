package dev.onion.aicoding.review;

import java.time.Instant;
import java.util.List;
import java.util.OptionalLong;

public record ReviewRecord(
        String id,
        Instant timestamp,
        List<String> changedFiles,
        String gitDiffHash,
        String projectAnalysisSnapshot,
        String projectMemorySnapshot,
        String provider,
        long elapsedTimeMillis,
        OptionalLong tokenUsage,
        String reviewText,
        String suggestedCodexPrompt) {

    public ReviewRecord {
        changedFiles = List.copyOf(changedFiles);
        tokenUsage = tokenUsage == null ? OptionalLong.empty() : tokenUsage;
    }
}
