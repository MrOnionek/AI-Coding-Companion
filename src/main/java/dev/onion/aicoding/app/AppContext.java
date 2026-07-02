package dev.onion.aicoding.app;

import dev.onion.aicoding.settings.Settings;
import dev.onion.aicoding.settings.SettingsManager;

public class AppContext {

    private final SettingsManager settingsManager;
    private final Settings settings;

    public AppContext() {
        this.settingsManager = new SettingsManager();
        this.settings = settingsManager.load();
    }

    public SettingsManager settingsManager() {
        return settingsManager;
    }

    public Settings settings() {
        return settings;
    }
}