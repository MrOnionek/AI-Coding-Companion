package dev.onion.aicoding.ui;

import dev.onion.aicoding.app.AppContext;
import dev.onion.aicoding.ai.AIProvider;
import dev.onion.aicoding.ai.AIService;
import dev.onion.aicoding.ai.AIRequest;
import dev.onion.aicoding.ai.AIResponse;
import dev.onion.aicoding.ai.AutomaticReviewPipeline;
import dev.onion.aicoding.project.ProjectAnalysis;
import dev.onion.aicoding.project.ProjectManager;
import dev.onion.aicoding.prompt.PromptBuilder;
import dev.onion.aicoding.prompt.ReviewPromptBuilder;
import dev.onion.aicoding.memory.MemoryManager;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainWindow {

    private Sidebar sidebar;
    private DiffViewer diffViewer;
    private ReviewPanel reviewPanel;
    private PromptPanel promptPanel;
    private StatusBar statusBar;
    private MemoryPanel memoryPanel;
    private Stage stage;
    private final ProjectManager projectManager;
    private final AIService aiService;
    private final PromptBuilder reviewPromptBuilder;
    private final MemoryManager memoryManager;
    private volatile ProjectAnalysis currentAnalysis;
    private final AutomaticReviewPipeline automaticReviewPipeline;

    public MainWindow(AppContext context) {
        this.projectManager = context.projectManager();
        this.aiService = context.aiService();
        this.reviewPromptBuilder = new ReviewPromptBuilder();
        this.memoryManager = context.memoryManager();
        this.automaticReviewPipeline = new AutomaticReviewPipeline(
                aiService, () -> currentAnalysis, memoryManager::currentMemory,
                projectManager::getCurrentDiff);
    }

    public void show(Stage stage) {
        this.stage = stage;
        sidebar = new Sidebar();
        diffViewer = new DiffViewer();
        reviewPanel = new ReviewPanel();
        promptPanel = new PromptPanel();
        statusBar = new StatusBar();
        memoryPanel = new MemoryPanel();
        promptPanel.setOnReview(this::requestReview);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #181818;");

        root.setLeft(sidebar);
        root.setCenter(diffViewer);
        VBox rightPanels = new VBox(reviewPanel, memoryPanel);
        VBox.setVgrow(reviewPanel, Priority.ALWAYS);
        VBox.setVgrow(memoryPanel, Priority.ALWAYS);
        root.setRight(rightPanels);
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
        subscribeToAutomaticReviews();
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
        memoryManager.onMemoryChanged(memory ->
                runOnUiThread(() -> memoryPanel.setMemory(memory)));
        projectManager.onProjectOpened(project -> {
            memoryManager.openProject(project.path());
            runOnUiThread(() ->
                    stage.setTitle("AI Coding Companion - " + project.path().getFileName()));
        });
        projectManager.onProjectClosed(() -> runOnUiThread(() ->
        {
            stage.setTitle("AI Coding Companion");
            currentAnalysis = null;
            sidebar.clearProjectSummary();
            memoryPanel.clear();
        }));
        projectManager.onProjectAnalyzed(analysis -> {
            memoryManager.recordAnalysis(analysis);
                runOnUiThread(() -> {
                    currentAnalysis = analysis;
                    sidebar.setProjectAnalysis(analysis);
                });
        });
        projectManager.onStatusChanged(status ->
                runOnUiThread(() -> statusBar.setStatus(status)));
        projectManager.onFileChanged(changedFile -> runOnUiThread(() -> {
            sidebar.addChangedFile(changedFile);
            automaticReviewPipeline.fileChanged(changedFile);
        }));
    }

    private void subscribeToAutomaticReviews() {
        automaticReviewPipeline.onReview(response -> runOnUiThread(() ->
                displayReview(response)));
        automaticReviewPipeline.onCodexPrompt(prompt -> runOnUiThread(() ->
                promptPanel.setPrompt(prompt)));
        automaticReviewPipeline.onDiff(diff -> runOnUiThread(() ->
                diffViewer.setDiff(diff)));
        automaticReviewPipeline.onStatusChanged(status -> runOnUiThread(() ->
                statusBar.setStatus(status)));
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
                AIRequest request = reviewPromptBuilder.build(currentAnalysis,
                        memoryManager.currentMemory(),
                        projectManager.getCurrentDiff(), userPrompt);
                response = aiService.send(request);
            } catch (Exception e) {
                response = new AIResponse("AI request failed: " + e.getMessage(),
                        java.time.Duration.ZERO, providerName,
                        java.util.OptionalLong.empty());
            }
            AIResponse completed = response;
            runOnUiThread(() -> {
                displayReview(completed);
                statusBar.setStatus("Provider: " + completed.providerName()
                        + " | Elapsed: " + completed.elapsedTime().toMillis() + " ms");
                promptPanel.setReviewInProgress(false);
            });
        }, "ai-review-request");
        requestThread.setDaemon(true);
        requestThread.start();
    }

    private void displayReview(AIResponse response) {
        reviewPanel.setReview("Provider: " + response.providerName()
                + "\nElapsed: " + response.elapsedTime().toMillis() + " ms\n\n"
                + response.responseText());
    }

    public void close() {
        automaticReviewPipeline.close();
    }

    private void runOnUiThread(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }
}
