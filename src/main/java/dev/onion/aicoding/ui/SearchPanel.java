package dev.onion.aicoding.ui;

import dev.onion.aicoding.search.ProjectSearch;
import dev.onion.aicoding.search.SearchResult;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class SearchPanel extends VBox implements AutoCloseable {

    private final ProjectSearch projectSearch;
    private final TextField query = new TextField();
    private final Label status = new Label("Indexing project...");
    private final ListView<SearchResult> results = new ListView<>();
    private final TextArea sourcePreview = new TextArea();
    private final ExecutorService worker = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "project-search-ui-worker");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicLong searchGeneration = new AtomicLong();

    public SearchPanel(ProjectSearch projectSearch) {
        this.projectSearch = projectSearch;
        setSpacing(8);
        setStyle("-fx-padding: 12; -fx-background-color: #202020;");
        query.setPromptText("Search classes, methods, packages, or files");
        query.textProperty().addListener((observable, oldValue, newValue) ->
                searchInBackground(newValue));
        results.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(SearchResult result, boolean empty) {
                super.updateItem(result, empty);
                setText(empty || result == null ? null
                        : result.name() + " · " + result.kind()
                        + "\n" + result.file() + ":" + result.declarationLine());
            }
        });
        results.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, selected) -> {
                    if (selected != null) {
                        openInBackground(selected);
                    }
                });
        sourcePreview.setEditable(false);
        sourcePreview.setWrapText(false);
        sourcePreview.setStyle("-fx-font-family: Consolas, monospace;");
        SplitPane content = new SplitPane(results, sourcePreview);
        content.setDividerPositions(0.35);
        getChildren().addAll(query, status, content);
        VBox.setVgrow(content, Priority.ALWAYS);
        projectSearch.onIndexUpdated(index -> Platform.runLater(() -> {
            status.setText("Indexed " + index.entries().size() + " symbols");
            searchInBackground(query.getText());
        }));
    }

    public void focusSearch() {
        query.requestFocus();
        query.selectAll();
    }

    private void searchInBackground(String text) {
        long version = searchGeneration.incrementAndGet();
        worker.submit(() -> {
            List<SearchResult> found = projectSearch.search(text);
            Platform.runLater(() -> {
                if (version == searchGeneration.get()) {
                    results.setItems(FXCollections.observableArrayList(found));
                    status.setText(found.size() + " result(s)");
                }
            });
        });
    }

    private void openInBackground(SearchResult result) {
        worker.submit(() -> {
            try {
                String content = Files.readString(result.file(), StandardCharsets.UTF_8);
                Platform.runLater(() -> showSource(content, result));
            } catch (Exception exception) {
                Platform.runLater(() ->
                        sourcePreview.setText("Could not open file: " + exception.getMessage()));
            }
        });
    }

    private void showSource(String content, SearchResult result) {
        sourcePreview.setText(content);
        int offset = offsetForLine(content, result.declarationLine());
        sourcePreview.positionCaret(offset);
        sourcePreview.setScrollTop(Math.max(0, result.declarationLine() - 3) * 16.0);
        status.setText(result.file() + " · line " + result.declarationLine());
    }

    private int offsetForLine(String content, int line) {
        int currentLine = 1;
        for (int index = 0; index < content.length(); index++) {
            if (currentLine == line) {
                return index;
            }
            if (content.charAt(index) == '\n') {
                currentLine++;
            }
        }
        return content.length();
    }

    @Override
    public void close() {
        searchGeneration.incrementAndGet();
        worker.shutdownNow();
    }
}
