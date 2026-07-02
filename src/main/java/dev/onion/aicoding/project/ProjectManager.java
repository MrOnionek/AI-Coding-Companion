package dev.onion.aicoding.project;

import dev.onion.aicoding.architecture.ArchitectureAnalyzer;
import dev.onion.aicoding.architecture.ArchitectureGraph;
import dev.onion.aicoding.git.GitService;
import dev.onion.aicoding.settings.Settings;
import dev.onion.aicoding.settings.SettingsManager;
import dev.onion.aicoding.watcher.FolderWatcher;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

public class ProjectManager {

    private static final int MAX_RECENT_PROJECTS = 10;
    private final Settings settings;
    private final SettingsManager settingsManager;
    private Project currentProject;
    private GitService gitService;
    private FolderWatcher folderWatcher;
    private Thread watcherThread;
    private Consumer<Project> onProjectOpened = project -> { };
    private Runnable onProjectClosed = () -> { };
    private Consumer<Path> onFileChanged = path -> { };
    private Consumer<String> onStatusChanged = status -> { };
    private Consumer<ProjectAnalysis> onProjectAnalyzed = analysis -> { };
    private Consumer<ArchitectureGraph> onArchitectureAnalyzed = graph -> { };

    public ProjectManager(Settings settings, SettingsManager settingsManager) {
        this.settings = settings;
        this.settingsManager = settingsManager;
    }

    public Optional<Project> open(Path path) {
        if (!isValidProjectFolder(path)) {
            onStatusChanged.accept("Project folder does not exist");
            return Optional.empty();
        }
        stopWatcher();
        currentProject = new Project(path.toAbsolutePath().normalize(),
                isGitRepository(path));
        gitService = new GitService(currentProject.path().toString());
        String projectPath = currentProject.path().toString();
        settings.setLastProjectPath(projectPath);
        settings.getRecentProjects().removeIf(recent -> recent.path().equals(projectPath));
        settings.getRecentProjects().add(0, new RecentProject(projectPath));
        if (settings.getRecentProjects().size() > MAX_RECENT_PROJECTS) {
            settings.getRecentProjects().subList(MAX_RECENT_PROJECTS,
                    settings.getRecentProjects().size()).clear();
        }
        settingsManager.save(settings);
        onProjectOpened.accept(currentProject);
        onStatusChanged.accept("Project opened: " + projectPath + " | "
                + gitStatus(currentProject));
        startWatcher();
        startIndexing(currentProject);
        return Optional.of(currentProject);
    }

    public Optional<Project> reopenLastProject() {
        String path = settings.getLastProjectPath();
        return path == null || path.isBlank() ? Optional.empty() : open(Path.of(path));
    }

    public Optional<Project> currentProject() {
        return Optional.ofNullable(currentProject);
    }

    public boolean isValidProjectFolder(Path path) {
        return path != null && Files.isDirectory(path);
    }

    public boolean isGitRepository(Path path) {
        return isValidProjectFolder(path) && Files.isDirectory(path.resolve(".git"));
    }

    public String getCurrentDiff() {
        if (currentProject == null || !currentProject.gitRepository()) {
            return "Git repository not detected.";
        }
        return gitService.getDiff();
    }

    public void closeProject() {
        stopWatcher();
        currentProject = null;
        gitService = null;
        onProjectClosed.run();
        onStatusChanged.accept("No project open");
    }

    private void startWatcher() {
        Project project = currentProject;
        try {
            folderWatcher = new FolderWatcher(project.path().toString(), changedFile -> {
                onFileChanged.accept(changedFile);
                onStatusChanged.accept("Detected: " + changedFile.getFileName());
            });
        } catch (IOException e) {
            onStatusChanged.accept("Watcher error: " + e.getMessage());
            return;
        }
        FolderWatcher watcher = folderWatcher;
        watcherThread = new Thread(() -> {
            try {
                onStatusChanged.accept("Watching project: " + project.path()
                        + " | " + gitStatus(project));
                watcher.startWatching();
            } catch (IOException e) {
                if (watcher == folderWatcher) {
                    onStatusChanged.accept("Watcher error: " + e.getMessage());
                }
            }
        }, "project-folder-watcher");
        watcherThread.setDaemon(true);
        watcherThread.start();
    }

    private void stopWatcher() {
        FolderWatcher watcher = folderWatcher;
        folderWatcher = null;
        watcherThread = null;
        if (watcher != null) {
            try {
                watcher.close();
            } catch (IOException e) {
                onStatusChanged.accept("Watcher shutdown error: " + e.getMessage());
            }
        }
    }

    private String gitStatus(Project project) {
        return project.gitRepository() ? "Git repo detected" : "Git repo missing";
    }

    private void startIndexing(Project project) {
        Thread indexerThread = new Thread(() -> {
            try {
                ProjectAnalysis analysis = new ProjectAnalyzer().analyze(project);
                if (project == currentProject) {
                    onProjectAnalyzed.accept(analysis);
                }
                ArchitectureGraph graph = new ArchitectureAnalyzer().analyze(project);
                if (project == currentProject) {
                    onArchitectureAnalyzed.accept(graph);
                }
            } catch (IOException e) {
                if (project == currentProject) {
                    onStatusChanged.accept("Indexing error: " + e.getMessage());
                }
            }
        }, "project-indexer");
        indexerThread.setDaemon(true);
        indexerThread.start();
    }

    public void onProjectOpened(Consumer<Project> callback) {
        onProjectOpened = callback;
    }

    public void onProjectClosed(Runnable callback) {
        onProjectClosed = callback;
    }

    public void onFileChanged(Consumer<Path> callback) {
        onFileChanged = callback;
    }

    public void onStatusChanged(Consumer<String> callback) {
        onStatusChanged = callback;
    }

    public void onProjectAnalyzed(Consumer<ProjectAnalysis> callback) {
        onProjectAnalyzed = callback;
    }

    public void onArchitectureAnalyzed(Consumer<ArchitectureGraph> callback) {
        onArchitectureAnalyzed = callback;
    }
}
