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
import dev.onion.aicoding.review.ReviewDatabase;
import dev.onion.aicoding.review.ReviewRecord;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
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
    private ReviewHistoryPanel reviewHistoryPanel;
    private ArchitecturePanel architecturePanel;
    private Stage stage;
    private final ProjectManager projectManager;
    private final AIService aiService;
    private final PromptBuilder reviewPromptBuilder;
    private final MemoryManager memoryManager;
    private final ReviewDatabase reviewDatabase;
    private final Set<String> changedFiles = new LinkedHashSet<>();
    private volatile String latestCodexPrompt = "";
    private volatile ProjectAnalysis currentAnalysis;
    private final AutomaticReviewPipeline automaticReviewPipeline;

    public MainWindow(AppContext context) {
        this.projectManager = context.projectManager();
        this.aiService = context.aiService();
        this.reviewPromptBuilder = new ReviewPromptBuilder();
        this.memoryManager = context.memoryManager();
        this.reviewDatabase = context.reviewDatabase();
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
        List<String> providerNames = aiService.getProviders().stream()
                .map(AIProvider::getName).toList();
        reviewHistoryPanel = new ReviewHistoryPanel(providerNames);
        architecturePanel = new ArchitecturePanel();
        reviewHistoryPanel.setSearch(reviewDatabase::search);
        reviewHistoryPanel.setOnReviewSelected(this::restoreReview);
        promptPanel.setOnReview(this::requestReview);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #181818;");

        root.setLeft(sidebar);
        root.setCenter(diffViewer);
        VBox rightPanels = new VBox(
                reviewPanel, memoryPanel, reviewHistoryPanel, architecturePanel);
        VBox.setVgrow(reviewPanel, Priority.ALWAYS);
        VBox.setVgrow(memoryPanel, Priority.ALWAYS);
        VBox.setVgrow(reviewHistoryPanel, Priority.ALWAYS);
        VBox.setVgrow(architecturePanel, Priority.ALWAYS);
        root.setRight(rightPanels);
        root.setBottom(promptPanel);

        BorderPane outer = new BorderPane();
        outer.setTop(new Toolbar(this::chooseProject,
                providerNames,
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
        reviewDatabase.onHistoryChanged(history ->
                runOnUiThread(() -> reviewHistoryPanel.setHistory(history)));
        projectManager.onProjectOpened(project -> {
            memoryManager.openProject(project.path());
            reviewDatabase.openProject(project.path());
            synchronized (changedFiles) {
                changedFiles.clear();
            }
            runOnUiThread(() ->
                    stage.setTitle("AI Coding Companion - " + project.path().getFileName()));
        });
        projectManager.onProjectClosed(() -> runOnUiThread(() ->
        {
            stage.setTitle("AI Coding Companion");
            currentAnalysis = null;
            sidebar.clearProjectSummary();
            memoryPanel.clear();
            architecturePanel.clear();
        }));
        projectManager.onProjectAnalyzed(analysis -> {
            memoryManager.recordAnalysis(analysis);
                runOnUiThread(() -> {
                    currentAnalysis = analysis;
                    sidebar.setProjectAnalysis(analysis);
                });
        });
        projectManager.onArchitectureAnalyzed(graph ->
                runOnUiThread(() -> architecturePanel.setGraph(graph)));
        projectManager.onStatusChanged(status ->
                runOnUiThread(() -> statusBar.setStatus(status)));
        projectManager.onFileChanged(changedFile -> runOnUiThread(() -> {
            sidebar.addChangedFile(changedFile);
            synchronized (changedFiles) {
                changedFiles.add(changedFile.toString());
            }
            automaticReviewPipeline.fileChanged(changedFile);
        }));
    }

    private void subscribeToAutomaticReviews() {
        automaticReviewPipeline.onReview(response -> {
            persistReview(response);
            runOnUiThread(() -> displayReview(response));
        });
        automaticReviewPipeline.onCodexPrompt(prompt -> {
            latestCodexPrompt = prompt;
            runOnUiThread(() -> promptPanel.setPrompt(prompt));
        });
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
            persistReview(completed);
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

    private void persistReview(AIResponse response) {
        ProjectAnalysis analysis = currentAnalysis;
        if (analysis == null) {
            return;
        }
        String diff = projectManager.getCurrentDiff();
        List<String> reviewedFiles;
        synchronized (changedFiles) {
            reviewedFiles = new ArrayList<>(changedFiles);
            changedFiles.clear();
        }
        ReviewRecord record = new ReviewRecord(
                Instant.now().toEpochMilli() + "-" + UUID.randomUUID(),
                Instant.now(), reviewedFiles, sha256(diff), analysis.toString(),
                memoryManager.currentMemory().toPromptText(), response.providerName(),
                response.elapsedTime().toMillis(), response.tokenUsage(),
                response.responseText(), latestCodexPrompt);
        reviewDatabase.save(record);
    }

    private void restoreReview(ReviewRecord review) {
        reviewPanel.setReview("Provider: " + review.provider()
                + "\nElapsed: " + review.elapsedTimeMillis() + " ms\n\n"
                + review.reviewText());
        promptPanel.setPrompt(review.suggestedCodexPrompt());
        statusBar.setStatus("Restored review from " + review.timestamp());
    }

    private String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
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
