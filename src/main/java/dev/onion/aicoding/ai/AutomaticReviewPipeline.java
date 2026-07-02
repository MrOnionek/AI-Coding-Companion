package dev.onion.aicoding.ai;

import dev.onion.aicoding.memory.ProjectMemory;
import dev.onion.aicoding.project.ProjectAnalysis;
import dev.onion.aicoding.prompt.CodexPromptBuilder;
import dev.onion.aicoding.prompt.ReviewPromptBuilder;
import dev.onion.aicoding.task.TaskPlan;
import java.nio.file.Path;
import java.time.Duration;
import java.util.OptionalLong;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import dev.onion.aicoding.settings.Settings;

public class AutomaticReviewPipeline implements AutoCloseable {

    private final Settings settings;
    private final AIService aiService;
    private final Supplier<ProjectAnalysis> analysisSupplier;
    private final Supplier<ProjectMemory> memorySupplier;
    private final Supplier<String> diffSupplier;
    private final Supplier<TaskPlan> taskSupplier;
    private final ReviewPromptBuilder reviewPromptBuilder = new ReviewPromptBuilder();
    private final CodexPromptBuilder codexPromptBuilder = new CodexPromptBuilder();
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(runnable ->
                    daemonThread(runnable, "review-debounce"));
    private final ExecutorService requestExecutor =
            Executors.newCachedThreadPool(runnable ->
                    daemonThread(runnable, "automatic-ai-review"));
    private final AtomicLong generation = new AtomicLong();
    private volatile Future<?> pendingDebounce;
    private volatile Future<?> runningReview;
    private BiConsumer<AIResponse, String> onReview = (response, diffSnapshot) -> { };
    private Consumer<String> onCodexPrompt = prompt -> { };
    private Consumer<String> onDiff = diff -> { };
    private Consumer<String> onStatusChanged = status -> { };

    public AutomaticReviewPipeline(
            AIService aiService,
            Settings settings,
            Supplier<ProjectAnalysis> analysisSupplier,
            Supplier<ProjectMemory> memorySupplier,
            Supplier<TaskPlan> taskSupplier,
            Supplier<String> diffSupplier) {
        this.aiService = aiService;
        this.settings = settings;
        this.analysisSupplier = analysisSupplier;
        this.memorySupplier = memorySupplier;
        this.taskSupplier = taskSupplier;
        this.diffSupplier = diffSupplier;
    }

    public synchronized void fileChanged(Path changedFile) {
        long version = generation.incrementAndGet();
        cancel(pendingDebounce);
        cancel(runningReview);
        if (!settings.isAutomaticReviewsEnabled()) {
            onStatusChanged.accept("Automatic reviews disabled");
            return;
        }
        onStatusChanged.accept("Change detected: " + changedFile.getFileName()
                + " | review pending");
        pendingDebounce = scheduler.schedule(
                () -> startReview(version, changedFile),
                settings.getReviewDebounceMillis(), TimeUnit.MILLISECONDS);
    }

    private void startReview(long version, Path changedFile) {
        if (!settings.isAutomaticReviewsEnabled()) {
            return;
        }
        ProjectAnalysis analysis = analysisSupplier.get();
        if (analysis == null || version != generation.get()) {
            return;
        }
        ProjectMemory memory = memorySupplier.get();
        TaskPlan taskPlan = taskSupplier.get();
        String diffSnapshot = diffSupplier.get();
        if (version != generation.get()) {
            return;
        }
        onDiff.accept(diffSnapshot);
        onStatusChanged.accept(diffSnapshot.isBlank()
                ? "Git diff is empty."
                : "Reviewing diff: " + diffSnapshot.length() + " characters");
        String requestText = "Review the latest change to " + changedFile.getFileName() + ".";
        AIRequest reviewRequest = reviewPromptBuilder.build(
                analysis, memory, taskPlan, diffSnapshot, requestText);
        AIRequest codexRequest = codexPromptBuilder.build(
                analysis, memory, taskPlan, diffSnapshot,
                "Implement fixes recommended for the latest project changes.");
        onCodexPrompt.accept(codexRequest.userPrompt());
        onStatusChanged.accept("Automatic review using "
                + aiService.getActiveProvider().getName() + "...");

        runningReview = requestExecutor.submit(() -> {
            AIResponse response;
            try {
                response = aiService.send(reviewRequest);
            } catch (Exception e) {
                response = new AIResponse("Automatic review failed: " + readableMessage(e),
                        Duration.ZERO, aiService.getActiveProvider().getName(),
                        OptionalLong.empty());
            }
            if (version == generation.get() && !Thread.currentThread().isInterrupted()) {
                onReview.accept(response, diffSnapshot);
                onStatusChanged.accept("Automatic review complete: "
                        + response.providerName() + " | "
                        + response.elapsedTime().toMillis() + " ms");
            }
        });
    }

    public void onReview(BiConsumer<AIResponse, String> callback) {
        onReview = callback;
    }

    public void onCodexPrompt(Consumer<String> callback) {
        onCodexPrompt = callback;
    }

    public void onStatusChanged(Consumer<String> callback) {
        onStatusChanged = callback;
    }

    public void onDiff(Consumer<String> callback) {
        onDiff = callback;
    }

    public synchronized void settingsChanged() {
        if (!settings.isAutomaticReviewsEnabled()) {
            generation.incrementAndGet();
            cancel(pendingDebounce);
            cancel(runningReview);
            onStatusChanged.accept("Automatic reviews disabled");
        }
    }

    @Override
    public synchronized void close() {
        generation.incrementAndGet();
        cancel(pendingDebounce);
        cancel(runningReview);
        scheduler.shutdownNow();
        requestExecutor.shutdownNow();
    }

    private void cancel(Future<?> future) {
        if (future != null) {
            future.cancel(true);
        }
    }

    private static Thread daemonThread(Runnable runnable, String name) {
        Thread thread = new Thread(runnable, name);
        thread.setDaemon(true);
        return thread;
    }

    private String readableMessage(Exception exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName() : exception.getMessage();
    }
}
