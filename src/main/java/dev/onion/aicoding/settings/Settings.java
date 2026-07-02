package dev.onion.aicoding.settings;

import dev.onion.aicoding.project.RecentProject;
import java.util.ArrayList;
import java.util.List;

public class Settings {

    private String lastProjectPath = "";
    private final List<RecentProject> recentProjects = new ArrayList<>();
    private boolean darkTheme = true;
    private boolean autoStartWatcher = true;

    public String getLastProjectPath() {
        return lastProjectPath;
    }

    public void setLastProjectPath(String lastProjectPath) {
        this.lastProjectPath = lastProjectPath;
    }

    public List<RecentProject> getRecentProjects() {
        return recentProjects;
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
