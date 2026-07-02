package dev.onion.aicoding.project;

import java.io.IOException;

/**
 * Compatibility wrapper for callers that only need the Milestone 3 summary.
 */
public class ProjectIndexer {

    public ProjectSummary index(Project project) throws IOException {
        return new ProjectAnalyzer().analyze(project).summary();
    }
}
