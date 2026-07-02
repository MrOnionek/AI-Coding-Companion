package dev.onion.aicoding.ui;

import dev.onion.aicoding.app.AppContext;
import dev.onion.aicoding.project.ProjectManager;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class MainWindow {

    private Sidebar sidebar;
    private DiffViewer diffViewer;
    private ReviewPanel reviewPanel;
    private PromptPanel promptPanel;
    private StatusBar statusBar;
    private Stage stage;
    private final ProjectManager projectManager;

    public MainWindow(AppContext context) {
        this.projectManager = context.projectManager();
    }

    public void show(Stage stage) {
        this.stage = stage;
        sidebar = new Sidebar();
        diffViewer = new DiffViewer();
        reviewPanel = new ReviewPanel();
        promptPanel = new PromptPanel();
        statusBar = new StatusBar();

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #181818;");

        root.setLeft(sidebar);
        root.setCenter(diffViewer);
        root.setRight(reviewPanel);
        root.setBottom(promptPanel);

        BorderPane outer = new BorderPane();
        outer.setTop(new Toolbar(this::chooseProject));
        outer.setCenter(root);
        outer.setBottom(statusBar);

        Scene scene = new Scene(outer, 1300, 800);

        stage.setTitle("AI Coding Companion");
        stage.setScene(scene);
        stage.show();

        subscribeToProjectEvents();
        projectManager.reopenLastProject();
    }

    private void chooseProject() {
        new ProjectChooser().choose(stage).ifPresent(projectManager::open);
    }

    private void subscribeToProjectEvents() {
        projectManager.onProjectOpened(project -> runOnUiThread(() ->
                stage.setTitle("AI Coding Companion - " + project.path().getFileName())));
        projectManager.onProjectClosed(() -> runOnUiThread(() ->
                stage.setTitle("AI Coding Companion")));
        projectManager.onStatusChanged(status ->
                runOnUiThread(() -> statusBar.setStatus(status)));
        projectManager.onFileChanged(changedFile -> runOnUiThread(() -> {
            sidebar.addChangedFile(changedFile);
            diffViewer.setDiff(projectManager.getCurrentDiff());
            reviewPanel.setReview("AI review not connected yet.");
            promptPanel.setPrompt("Next step: connect OpenAI API.");
        }));
    }

    private void runOnUiThread(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }
}
