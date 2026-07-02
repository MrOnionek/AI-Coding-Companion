package dev.onion.aicoding.settings;

import dev.onion.aicoding.project.RecentProject;
import java.util.ArrayList;
import java.util.List;

public class Settings {

    public enum Theme {
        DARK,
        LIGHT,
        SYSTEM
    }

    private String lastProjectPath = "";
    private final List<RecentProject> recentProjects = new ArrayList<>();
    private volatile String openAIApiKeyOverride = "";
    private volatile String defaultAIProvider = "ChatGPT";
    private volatile boolean automaticReviewsEnabled = true;
    private volatile long reviewDebounceMillis = 1500;
    private volatile int reviewTimeoutSeconds = 120;
    private volatile int uiFontSize = 13;
    private volatile Theme theme = Theme.SYSTEM;

    public String getLastProjectPath() {
        return lastProjectPath;
    }

    public void setLastProjectPath(String lastProjectPath) {
        this.lastProjectPath = lastProjectPath;
    }

    public List<RecentProject> getRecentProjects() {
        return recentProjects;
    }

    public String getOpenAIApiKeyOverride() {
        return openAIApiKeyOverride;
    }

    public void setOpenAIApiKeyOverride(String value) {
        openAIApiKeyOverride = value == null ? "" : value.strip();
    }

    public String resolveOpenAIApiKey() {
        return openAIApiKeyOverride.isBlank()
                ? System.getenv("OPENAI_API_KEY") : openAIApiKeyOverride;
    }

    public String getDefaultAIProvider() {
        return defaultAIProvider;
    }

    public void setDefaultAIProvider(String value) {
        defaultAIProvider = value == null || value.isBlank() ? "ChatGPT" : value;
    }

    public boolean isAutomaticReviewsEnabled() {
        return automaticReviewsEnabled;
    }

    public void setAutomaticReviewsEnabled(boolean value) {
        automaticReviewsEnabled = value;
    }

    public long getReviewDebounceMillis() {
        return reviewDebounceMillis;
    }

    public void setReviewDebounceMillis(long value) {
        reviewDebounceMillis = Math.max(250, value);
    }

    public int getReviewTimeoutSeconds() {
        return reviewTimeoutSeconds;
    }

    public void setReviewTimeoutSeconds(int value) {
        reviewTimeoutSeconds = Math.max(5, value);
    }

    public int getUiFontSize() {
        return uiFontSize;
    }

    public void setUiFontSize(int value) {
        uiFontSize = Math.max(8, value);
    }

    public Theme getTheme() {
        return theme;
    }

    public void setTheme(Theme value) {
        theme = value == null ? Theme.SYSTEM : value;
    }
}
