package dev.onion.aicoding.review;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ReviewDatabase {

    private final ReviewStore store;
    private final ReviewSearch search = new ReviewSearch();
    private final List<ReviewRecord> reviews = new ArrayList<>();
    private Path projectPath;
    private Consumer<ReviewHistory> onHistoryChanged = history -> { };

    public ReviewDatabase() {
        this(new ReviewStore());
    }

    public ReviewDatabase(ReviewStore store) {
        this.store = store;
    }

    public synchronized void openProject(Path path) {
        projectPath = path.toAbsolutePath().normalize();
        reviews.clear();
        try {
            reviews.addAll(store.load(projectPath).reviews());
        } catch (IOException ignored) {
            // Review history is optional and must not prevent project opening.
        }
        onHistoryChanged.accept(history());
    }

    public synchronized void save(ReviewRecord review) {
        if (projectPath == null) {
            return;
        }
        try {
            store.save(projectPath, review);
            reviews.add(review);
            onHistoryChanged.accept(history());
        } catch (IOException ignored) {
            // Keep review display functional if persistence is temporarily unavailable.
        }
    }

    public synchronized ReviewHistory history() {
        return new ReviewHistory(reviews);
    }

    public synchronized ReviewHistory search(String query, String provider) {
        return search.search(history(), query, provider);
    }

    public void onHistoryChanged(Consumer<ReviewHistory> callback) {
        onHistoryChanged = callback;
    }
}
