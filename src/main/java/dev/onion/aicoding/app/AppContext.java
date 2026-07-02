package dev.onion.aicoding.app;

import dev.onion.aicoding.settings.Settings;
import dev.onion.aicoding.settings.SettingsManager;
import dev.onion.aicoding.project.ProjectManager;

public class AppContext {

    private final SettingsManager settingsManager;
    private final Settings settings;
    private final ProjectManager projectManager;

    public AppContext() {
        this.settingsManager = new SettingsManager();
        this.settings = settingsManager.load();
        this.projectManager = new ProjectManager(settings, settingsManager);
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
}
