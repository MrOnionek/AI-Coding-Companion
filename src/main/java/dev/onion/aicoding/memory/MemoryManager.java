package dev.onion.aicoding.memory;

import dev.onion.aicoding.project.ProjectAnalysis;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class MemoryManager {

    private final MemoryStore store;
    private final List<MemoryEntry> entries = new ArrayList<>();
    private Path projectPath;
    private Consumer<ProjectMemory> onMemoryChanged = memory -> { };

    public MemoryManager() {
        this(new MemoryStore());
    }

    public MemoryManager(MemoryStore store) {
        this.store = store;
    }

    public synchronized ProjectMemory openProject(Path path) {
        projectPath = path.toAbsolutePath().normalize();
        entries.clear();
        try {
            entries.addAll(store.load(projectPath).entries());
        } catch (IOException ignored) {
            // A project remains usable if its optional memory cannot be loaded.
        }
        ProjectMemory memory = snapshot();
        onMemoryChanged.accept(memory);
        scanMarkersAsync(projectPath);
        return memory;
    }

    public synchronized void recordAnalysis(ProjectAnalysis analysis) {
        record(MemoryEntry.Type.ARCHITECTURE_DECISION,
                "Build system: " + analysis.summary().projectInfo().buildSystem()
                        + "; technologies: " + analysis.detectedTechnologies());
        record(MemoryEntry.Type.COMPLETED_MILESTONE,
                "Project analysis completed for "
                        + analysis.summary().projectInfo().name());
        record(MemoryEntry.Type.CODING_CONVENTION,
                "Primary language is Java; packages: "
                        + analysis.summary().packageNames());
    }

    public synchronized void record(MemoryEntry.Type type, String content) {
        if (projectPath == null || content == null || content.isBlank()
                || entries.stream().anyMatch(entry ->
                entry.type() == type && entry.content().equals(content))) {
            return;
        }
        entries.add(new MemoryEntry(type, content, Instant.now()));
        saveAndNotify();
    }

    public synchronized ProjectMemory currentMemory() {
        return snapshot();
    }

    public void onMemoryChanged(Consumer<ProjectMemory> callback) {
        onMemoryChanged = callback;
    }

    private void scanMarkersAsync(Path path) {
        Thread scanner = new Thread(() -> {
            try (Stream<Path> files = Files.walk(path)) {
                files.filter(file -> file.toString().endsWith(".java"))
                        .filter(file -> !file.toString().contains(
                                java.io.File.separator + "build" + java.io.File.separator))
                        .forEach(file -> scanFile(path, file));
            } catch (IOException ignored) {
                // Marker discovery is best-effort and must not block project opening.
            }
        }, "project-memory-scanner");
        scanner.setDaemon(true);
        scanner.start();
    }

    private void scanFile(Path root, Path file) {
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (int index = 0; index < lines.size(); index++) {
                String line = lines.get(index).trim();
                String location = root.relativize(file) + ":" + (index + 1) + " ";
                if (line.contains("TODO") || line.contains("FIXME")) {
                    record(MemoryEntry.Type.TODO, location + line);
                }
                if (line.contains("BUG")) {
                    record(MemoryEntry.Type.KNOWN_BUG, location + line);
                }
            }
        } catch (IOException ignored) {
            // Skip unreadable source files.
        }
    }

    private void saveAndNotify() {
        ProjectMemory memory = snapshot();
        try {
            store.save(projectPath, memory);
        } catch (IOException ignored) {
            // Keep in-memory data available if persistence temporarily fails.
        }
        onMemoryChanged.accept(memory);
    }

    private ProjectMemory snapshot() {
        return new ProjectMemory(entries);
    }
}
