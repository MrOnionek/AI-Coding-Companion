package dev.onion.aicoding.app;

import dev.onion.aicoding.ai.AIService;
import dev.onion.aicoding.settings.Settings;
import dev.onion.aicoding.settings.SettingsManager;
import dev.onion.aicoding.project.ProjectManager;
import dev.onion.aicoding.memory.MemoryManager;

public class AppContext {

    private final SettingsManager settingsManager;
    private final Settings settings;
    private final ProjectManager projectManager;
    private final AIService aiService;
    private final MemoryManager memoryManager;

    public AppContext() {
        this.settingsManager = new SettingsManager();
        this.settings = settingsManager.load();
        this.projectManager = new ProjectManager(settings, settingsManager);
        this.aiService = new AIService();
        this.memoryManager = new MemoryManager();
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
}
