package dev.onion.aicoding.settings;

import dev.onion.aicoding.project.RecentProject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class SettingsManager {

    private static final String RECENT_SEPARATOR = "\u001F";
    private final Path settingsFile = Path.of(
            System.getProperty("user.home"), ".ai-coding-companion", "settings.properties");

    public Settings load() {
        Settings settings = new Settings();
        if (!Files.isRegularFile(settingsFile)) {
            return settings;
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(settingsFile)) {
            properties.load(input);
            settings.setLastProjectPath(properties.getProperty("lastProjectPath", ""));
            settings.setOpenAIApiKeyOverride(
                    properties.getProperty("openAIApiKeyOverride", ""));
            settings.setDefaultAIProvider(
                    properties.getProperty("defaultAIProvider", "ChatGPT"));
            settings.setAutomaticReviewsEnabled(Boolean.parseBoolean(
                    properties.getProperty("automaticReviewsEnabled", "true")));
            settings.setReviewDebounceMillis(longValue(properties,
                    "reviewDebounceMillis", 1500));
            settings.setReviewTimeoutSeconds((int) longValue(properties,
                    "reviewTimeoutSeconds", 120));
            settings.setUiFontSize((int) longValue(properties, "uiFontSize", 13));
            try {
                settings.setTheme(Settings.Theme.valueOf(
                        properties.getProperty("theme", "SYSTEM")));
            } catch (IllegalArgumentException ignored) {
                settings.setTheme(Settings.Theme.SYSTEM);
            }
            String recent = properties.getProperty("recentProjects", "");
            if (!recent.isBlank()) {
                for (String path : recent.split(RECENT_SEPARATOR)) {
                    if (!path.isBlank()) {
                        settings.getRecentProjects().add(new RecentProject(path));
                    }
                }
            }
        } catch (IOException ignored) {
            // Invalid settings should not prevent the application from starting.
        }
        return settings;
    }

    public void save(Settings settings) {
        Properties properties = new Properties();
        properties.setProperty("lastProjectPath", settings.getLastProjectPath());
        properties.setProperty("openAIApiKeyOverride",
                settings.getOpenAIApiKeyOverride());
        properties.setProperty("defaultAIProvider", settings.getDefaultAIProvider());
        properties.setProperty("automaticReviewsEnabled",
                Boolean.toString(settings.isAutomaticReviewsEnabled()));
        properties.setProperty("reviewDebounceMillis",
                Long.toString(settings.getReviewDebounceMillis()));
        properties.setProperty("reviewTimeoutSeconds",
                Integer.toString(settings.getReviewTimeoutSeconds()));
        properties.setProperty("uiFontSize",
                Integer.toString(settings.getUiFontSize()));
        properties.setProperty("theme", settings.getTheme().name());
        properties.setProperty("recentProjects", settings.getRecentProjects().stream()
                .map(RecentProject::path)
                .reduce((left, right) -> left + RECENT_SEPARATOR + right)
                .orElse(""));
        try {
            Files.createDirectories(settingsFile.getParent());
            try (OutputStream output = Files.newOutputStream(settingsFile)) {
                properties.store(output, "AI Coding Companion settings");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not save settings", e);
        }
    }

    private long longValue(Properties properties, String key, long fallback) {
        try {
            return Long.parseLong(properties.getProperty(key, Long.toString(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
