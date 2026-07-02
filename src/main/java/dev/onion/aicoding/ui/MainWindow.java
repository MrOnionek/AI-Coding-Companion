package dev.onion.aicoding.ui;

import dev.onion.aicoding.app.AppContext;
import dev.onion.aicoding.ai.AIProvider;
import dev.onion.aicoding.ai.AIService;
import dev.onion.aicoding.ai.AIRequest;
import dev.onion.aicoding.ai.AIResponse;
import dev.onion.aicoding.project.ProjectAnalysis;
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
    private final AIService aiService;
    private volatile ProjectAnalysis currentAnalysis;

    public MainWindow(AppContext context) {
        this.projectManager = context.projectManager();
        this.aiService = context.aiService();
    }

    public void show(Stage stage) {
        this.stage = stage;
        sidebar = new Sidebar();
        diffViewer = new DiffViewer();
        reviewPanel = new ReviewPanel();
        promptPanel = new PromptPanel();
        statusBar = new StatusBar();
        promptPanel.setOnReview(this::requestReview);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #181818;");

        root.setLeft(sidebar);
        root.setCenter(diffViewer);
        root.setRight(reviewPanel);
        root.setBottom(promptPanel);

        BorderPane outer = new BorderPane();
        outer.setTop(new Toolbar(this::chooseProject,
                aiService.getProviders().stream().map(AIProvider::getName).toList(),
                aiService.getActiveProvider().getName(), this::selectProvider));
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

    private void selectProvider(String providerName) {
        if (aiService.setActiveProvider(providerName)) {
            statusBar.setStatus("AI provider selected: " + providerName);
        }
    }

    private void subscribeToProjectEvents() {
        projectManager.onProjectOpened(project -> runOnUiThread(() ->
                stage.setTitle("AI Coding Companion - " + project.path().getFileName())));
        projectManager.onProjectClosed(() -> runOnUiThread(() ->
        {
            stage.setTitle("AI Coding Companion");
            currentAnalysis = null;
            sidebar.clearProjectSummary();
        }));
        projectManager.onProjectAnalyzed(analysis ->
                runOnUiThread(() -> {
                    currentAnalysis = analysis;
                    sidebar.setProjectAnalysis(analysis);
                }));
        projectManager.onStatusChanged(status ->
                runOnUiThread(() -> statusBar.setStatus(status)));
        projectManager.onFileChanged(changedFile -> runOnUiThread(() -> {
            sidebar.addChangedFile(changedFile);
            diffViewer.setDiff(projectManager.getCurrentDiff());
            reviewPanel.setReview("AI review not connected yet.");
            promptPanel.setPrompt("Next step: connect OpenAI API.");
        }));
    }

    private void requestReview() {
        if (currentAnalysis == null) {
            reviewPanel.setReview("Open and index a project before requesting a review.");
            return;
        }
        String userPrompt = promptPanel.getPrompt();
        String providerName = aiService.getActiveProvider().getName();
        promptPanel.setReviewInProgress(true);
        statusBar.setStatus("Requesting review from " + providerName + "...");
        Thread requestThread = new Thread(() -> {
            AIResponse response;
            try {
                response = aiService.send(new AIRequest(
                        "You are a senior Java code reviewer. Give a concise, actionable review.",
                        userPrompt, formatAnalysis(currentAnalysis),
                        projectManager.getCurrentDiff()));
            } catch (Exception e) {
                response = new AIResponse("AI request failed: " + e.getMessage(),
                        java.time.Duration.ZERO, providerName,
                        java.util.OptionalLong.empty());
            }
            AIResponse completed = response;
            runOnUiThread(() -> {
                reviewPanel.setReview("Provider: " + completed.providerName()
                        + "\nElapsed: " + completed.elapsedTime().toMillis() + " ms\n\n"
                        + completed.responseText());
                statusBar.setStatus("Provider: " + completed.providerName()
                        + " | Elapsed: " + completed.elapsedTime().toMillis() + " ms");
                promptPanel.setReviewInProgress(false);
            });
        }, "ai-review-request");
        requestThread.setDaemon(true);
        requestThread.start();
    }

    private String formatAnalysis(ProjectAnalysis analysis) {
        var summary = analysis.summary();
        var info = summary.projectInfo();
        return """
                Project: %s
                Path: %s
                Git repository: %s
                Build system: %s
                Technologies: %s
                Java files: %d
                Packages: %s
                Declarations: %s
                Entry points: %s
                Important classes: %s
                Largest files: %s
                """.formatted(info.name(), info.absolutePath(), info.gitRepository(),
                info.buildSystem(), analysis.detectedTechnologies(),
                summary.totalJavaFiles(), summary.packageNames(),
                analysis.declarationCounts(), analysis.entryPoints(),
                analysis.importantClasses(), analysis.largestJavaFiles());
    }

    private void runOnUiThread(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }
}
