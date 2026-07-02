package dev.onion.aicoding.project;

import dev.onion.aicoding.model.ProjectInfo;
import java.util.List;

public record ProjectSummary(
        ProjectInfo projectInfo,
        long totalJavaFiles,
        int totalPackages,
        List<String> packageNames) {

    public ProjectSummary {
        packageNames = List.copyOf(packageNames);
    }
}
