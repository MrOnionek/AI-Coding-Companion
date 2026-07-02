package dev.onion.aicoding.settings;

public class SettingsManager {

    public Settings load() {
        return new Settings();
    }

    public void save(Settings settings) {
        // Settings persistence will be added later.
    }
}