package dev.onion.aicoding.settings;

public class Settings {

    private String lastProjectPath = "";
    private boolean darkTheme = true;
    private boolean autoStartWatcher = true;

    public String getLastProjectPath() {
        return lastProjectPath;
    }

    public void setLastProjectPath(String lastProjectPath) {
        this.lastProjectPath = lastProjectPath;
    }

    public boolean isDarkTheme() {
        return darkTheme;
    }

    public void setDarkTheme(boolean darkTheme) {
        this.darkTheme = darkTheme;
    }

    public boolean isAutoStartWatcher() {
        return autoStartWatcher;
    }

    public void setAutoStartWatcher(boolean autoStartWatcher) {
        this.autoStartWatcher = autoStartWatcher;
    }
}