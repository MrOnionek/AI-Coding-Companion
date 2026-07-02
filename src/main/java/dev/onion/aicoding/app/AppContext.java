package dev.onion.aicoding.app;

import dev.onion.aicoding.ai.AIService;
import dev.onion.aicoding.settings.Settings;
import dev.onion.aicoding.settings.SettingsManager;
import dev.onion.aicoding.project.ProjectManager;
import dev.onion.aicoding.memory.MemoryManager;
import dev.onion.aicoding.review.ReviewDatabase;
import dev.onion.aicoding.task.TaskStore;

public class AppContext {

    private final SettingsManager settingsManager;
    private final Settings settings;
    private final ProjectManager projectManager;
    private final AIService aiService;
    private final MemoryManager memoryManager;
    private final ReviewDatabase reviewDatabase;
    private final TaskStore taskStore;

    public AppContext() {
        this.settingsManager = new SettingsManager();
        this.settings = settingsManager.load();
        this.projectManager = new ProjectManager(settings, settingsManager);
        this.aiService = new AIService(settings);
        this.memoryManager = new MemoryManager();
        this.reviewDatabase = new ReviewDatabase();
        this.taskStore = new TaskStore();
    }

    public SettingsManager settingsManager() {
        return settingsManager;
    }

    public Settings settings() {
        return settings;
    }

    public ProjectManager projectManager() {
        return projectManager;
    }

    public AIService aiService() {
        return aiService;
    }

    public MemoryManager memoryManager() {
        return memoryManager;
    }

    public ReviewDatabase reviewDatabase() {
        return reviewDatabase;
    }

    public TaskStore taskStore() {
        return taskStore;
    }
}
