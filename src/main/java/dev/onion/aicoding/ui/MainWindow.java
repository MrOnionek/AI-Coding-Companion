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
import dev.onion.aicoding.memory.MemoryEntry;
import dev.onion.aicoding.review.ReviewDatabase;
import dev.onion.aicoding.review.ReviewRecord;
import dev.onion.aicoding.task.TaskPlanner;
import dev.onion.aicoding.task.TaskStore;
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
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
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
    private TaskPanel taskPanel;
    private Stage stage;
    private final ProjectManager projectManager;
    private final AIService aiService;
    private final PromptBuilder reviewPromptBuilder;
    private final MemoryManager memoryManager;
    private final ReviewDatabase reviewDatabase;
    private final TaskStore taskStore;
    private final TaskPlanner taskPlanner = new TaskPlanner();
    private final Set<String> changedFiles = new LinkedHashSet<>();
    private final AtomicBoolean manualReviewRunning = new AtomicBoolean();
    private volatile String latestCodexPrompt = "";
    private volatile ProjectAnalysis currentAnalysis;
    private final AutomaticReviewPipeline automaticReviewPipeline;

    public MainWindow(AppContext context) {
        this.projectManager = context.projectManager();
        this.aiService = context.aiService();
        this.reviewPromptBuilder = new ReviewPromptBuilder();
        this.memoryManager = context.memoryManager();
        this.reviewDatabase = context.reviewDatabase();
        this.taskStore = context.taskStore();
        this.automaticReviewPipeline = new AutomaticReviewPipeline(
                aiService, () -> currentAnalysis, memoryManager::currentMemory,
                taskStore::openTaskPlan,
                projectManager::getCurrentDiff);
    }

    public void show(Stage stage) {
        this.stage = stage;
        sidebar = new Sidebar();
        diffViewer = new DiffViewer();
        reviewPanel = new ReviewPanel();
        reviewPanel.setOnSaveToMemory(review -> {
            memoryManager.record(MemoryEntry.Type.REVIEW_NOTE, review);
            statusBar.setStatus("Review saved to project memory");
        });
        promptPanel = new PromptPanel();
        statusBar = new StatusBar();
        memoryPanel = new MemoryPanel();
        List<String> providerNames = aiService.getProviders().stream()
                .map(AIProvider::getName).toList();
        reviewHistoryPanel = new ReviewHistoryPanel(providerNames);
        architecturePanel = new ArchitecturePanel();
        taskPanel = new TaskPanel();
        taskPanel.setOnStatusChanged(taskStore::updateStatus);
        reviewHistoryPanel.setSearch(reviewDatabase::search);
        reviewHistoryPanel.setOnReviewSelected(this::restoreReview);
        promptPanel.setOnReview(this::requestReview);

        TabPane detailsTabs = new TabPane(
                tab("Review", reviewPanel),
                tab("Memory", memoryPanel),
                tab("History", reviewHistoryPanel),
                tab("Architecture", architecturePanel),
                tab("Tasks", taskPanel));
        detailsTabs.setMinWidth(300);

        SplitPane workspace = new SplitPane(sidebar, diffViewer, detailsTabs);
        workspace.setDividerPositions(0.22, 0.70);

        SplitPane root = new SplitPane(workspace, promptPanel);
        root.setOrientation(Orientation.VERTICAL);
        root.setDividerPositions(0.76);
        root.setStyle("-fx-background-color: #181818;");

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

    private Tab tab(String title, javafx.scene.Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
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
        taskStore.onTasksChanged(tasks ->
                runOnUiThread(() -> taskPanel.setTasks(tasks)));
        projectManager.onProjectOpened(project -> {
            memoryManager.openProject(project.path());
            reviewDatabase.openProject(project.path());
            taskStore.openProject(project.path());
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
            reviewPanel.setStatus(ReviewPanel.ReviewStatus.WAITING);
            automaticReviewPipeline.fileChanged(changedFile);
        }));
    }

    private void subscribeToAutomaticReviews() {
        automaticReviewPipeline.onReview(response -> {
            persistReview(response);
            runOnUiThread(() -> {
                displayReview(response);
                reviewPanel.setStatus(responseFailed(response)
                        ? ReviewPanel.ReviewStatus.FAILED
                        : ReviewPanel.ReviewStatus.COMPLETE);
            });
        });
        automaticReviewPipeline.onCodexPrompt(prompt -> {
            latestCodexPrompt = prompt;
            runOnUiThread(() -> {
                promptPanel.setPrompt(prompt);
                reviewPanel.setSuggestedPrompt(prompt);
            });
        });
        automaticReviewPipeline.onDiff(diff -> runOnUiThread(() ->
                diffViewer.setDiff(diff)));
        automaticReviewPipeline.onStatusChanged(status -> runOnUiThread(() -> {
            statusBar.setStatus(status);
            if (status.startsWith("Automatic review using")) {
                reviewPanel.setStatus(ReviewPanel.ReviewStatus.REVIEWING);
            }
        }));
    }

    private void requestReview() {
        if (currentAnalysis == null) {
            reviewPanel.setReview("Open and index a project before requesting a review.");
            reviewPanel.setStatus(ReviewPanel.ReviewStatus.FAILED);
            return;
        }
        if (!manualReviewRunning.compareAndSet(false, true)) {
            return;
        }
        String userPrompt = promptPanel.getPrompt();
        String providerName = aiService.getActiveProvider().getName();
        promptPanel.setReviewInProgress(true);
        reviewPanel.setStatus(ReviewPanel.ReviewStatus.REVIEWING);
        statusBar.setStatus("Requesting review from " + providerName + "...");
        Thread requestThread = new Thread(() -> {
            AIResponse response;
            try {
                AIRequest request = reviewPromptBuilder.build(currentAnalysis,
                        memoryManager.currentMemory(),
                        taskStore.openTaskPlan(),
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
                reviewPanel.setStatus(responseFailed(completed)
                        ? ReviewPanel.ReviewStatus.FAILED
                        : ReviewPanel.ReviewStatus.COMPLETE);
                statusBar.setStatus("Provider: " + completed.providerName()
                        + " | Elapsed: " + completed.elapsedTime().toMillis() + " ms");
                promptPanel.setReviewInProgress(false);
                manualReviewRunning.set(false);
            });
        }, "ai-review-request");
        requestThread.setDaemon(true);
        requestThread.start();
    }

    private void displayReview(AIResponse response) {
        reviewPanel.setMetadata(response.providerName(), response.elapsedTime().toMillis());
        reviewPanel.setReview(response.responseText());
    }

    private boolean responseFailed(AIResponse response) {
        String text = response.responseText().toLowerCase(java.util.Locale.ROOT);
        return text.contains("request failed") || text.contains("review failed")
                || text.contains("request was interrupted");
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
        taskStore.save(taskPlanner.createPlan(record));
    }

    private void restoreReview(ReviewRecord review) {
        reviewPanel.setMetadata(review.provider(), review.elapsedTimeMillis());
        reviewPanel.setReview(review.reviewText());
        reviewPanel.setStatus(ReviewPanel.ReviewStatus.COMPLETE);
        reviewPanel.setSuggestedPrompt(review.suggestedCodexPrompt());
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
