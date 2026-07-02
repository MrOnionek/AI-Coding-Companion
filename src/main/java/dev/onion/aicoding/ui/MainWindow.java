package dev.onion.aicoding.ui;

import dev.onion.aicoding.git.GitService;
import dev.onion.aicoding.watcher.FolderWatcher;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class MainWindow extends Application {

    private static final String PROJECT_PATH =
            "C:\\Users\\Szymon\\Downloads\\myminecraft-movement-aware-builder";

    private Sidebar sidebar;
    private DiffViewer diffViewer;
    private ReviewPanel reviewPanel;
    private PromptPanel promptPanel;
    private StatusBar statusBar;

    @Override
    public void start(Stage stage) {
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
        outer.setCenter(root);
        outer.setBottom(statusBar);

        Scene scene = new Scene(outer, 1300, 800);

        stage.setTitle("AI Coding Companion");
        stage.setScene(scene);
        stage.show();

        startWatcher();
    }

    private void startWatcher() {
        Thread watcherThread = new Thread(() -> {
            try {
                GitService gitService = new GitService(PROJECT_PATH);

                FolderWatcher watcher = new FolderWatcher(
                        PROJECT_PATH,
                        changedFile -> Platform.runLater(() -> {
                            sidebar.addChangedFile(changedFile);
                            statusBar.setStatus("Detected: " + changedFile.getFileName());

                            String diff = gitService.getDiff();
                            diffViewer.setDiff(diff);

                            reviewPanel.setReview("AI review not connected yet.");
                            promptPanel.setPrompt("Next step: connect OpenAI API.");
                        })
                );

                Platform.runLater(() -> statusBar.setStatus("Watching: " + PROJECT_PATH));
                watcher.startWatching();

            } catch (Exception e) {
                Platform.runLater(() -> statusBar.setStatus("Watcher error: " + e.getMessage()));
            }
        });

        watcherThread.setDaemon(true);
        watcherThread.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}