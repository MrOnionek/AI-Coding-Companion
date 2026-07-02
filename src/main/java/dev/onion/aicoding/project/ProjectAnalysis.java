package dev.onion.aicoding.project;

import java.util.List;
import java.util.Map;

public record ProjectAnalysis(
        ProjectSummary summary,
        Map<String, Integer> declarationCounts,
        List<String> detectedTechnologies,
        List<String> entryPoints,
        List<FileRank> largestJavaFiles,
        List<ImportantClass> importantClasses) {

    public ProjectAnalysis {
        declarationCounts = Map.copyOf(declarationCounts);
        detectedTechnologies = List.copyOf(detectedTechnologies);
        entryPoints = List.copyOf(entryPoints);
        largestJavaFiles = List.copyOf(largestJavaFiles);
        importantClasses = List.copyOf(importantClasses);
    }

    public record FileRank(String path, int lines) {
    }

    public record ImportantClass(String name, String path, int referencedByFiles,
                                 List<String> reasons) {
        public ImportantClass {
            reasons = List.copyOf(reasons);
        }
    }
}
