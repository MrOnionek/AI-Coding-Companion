package dev.onion.aicoding.ui;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class DiffViewer extends VBox {

    private final ListView<DiffLine> diffLines = new ListView<>();
    private final CheckBox wrapLines = new CheckBox("Wrap lines");
    private final AtomicLong parseGeneration = new AtomicLong();
    private final ExecutorService parser = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "git-diff-parser");
        thread.setDaemon(true);
        return thread;
    });

    public DiffViewer() {
        setSpacing(10);
        setStyle("-fx-padding: 12; -fx-background-color: #181818;");

        Label title = new Label("Git Diff");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; "
                + "-fx-font-weight: bold;");
        wrapLines.setStyle("-fx-text-fill: white;");
        wrapLines.setOnAction(event -> diffLines.refresh());

        diffLines.setStyle("-fx-font-family: Consolas, monospace; -fx-font-size: 13px;");
        diffLines.setCellFactory(list -> new DiffLineCell());
        diffLines.setItems(FXCollections.observableArrayList(
                new DiffLine("Git diff is empty.", LineType.CONTEXT)));

        getChildren().addAll(new HBox(12, title, wrapLines), diffLines);
        VBox.setVgrow(diffLines, Priority.ALWAYS);
    }

    public void setDiff(String diff) {
        long generation = parseGeneration.incrementAndGet();
        parser.submit(() -> {
            List<DiffLine> parsed = parse(diff);
            if (generation != parseGeneration.get()) {
                return;
            }
            var items = FXCollections.observableArrayList(parsed);
            Platform.runLater(() -> {
                if (generation == parseGeneration.get()) {
                    diffLines.setItems(items);
                }
            });
        });
    }

    private List<DiffLine> parse(String diff) {
        if (diff == null || diff.isBlank()) {
            return List.of(new DiffLine("Git diff is empty.", LineType.CONTEXT));
        }
        return Arrays.stream(diff.split("\\R", -1))
                .map(line -> new DiffLine(line, classify(line)))
                .toList();
    }

    private LineType classify(String line) {
        if (line.startsWith("diff --git ") || line.startsWith("--- ")
                || line.startsWith("+++ ") || line.startsWith("index ")) {
            return LineType.FILE_HEADER;
        }
        if (line.startsWith("@@")) {
            return LineType.HUNK_HEADER;
        }
        if (line.startsWith("+")) {
            return LineType.ADDED;
        }
        if (line.startsWith("-")) {
            return LineType.REMOVED;
        }
        return LineType.CONTEXT;
    }

    private final class DiffLineCell extends ListCell<DiffLine> {
        @Override
        protected void updateItem(DiffLine line, boolean empty) {
            super.updateItem(line, empty);
            if (empty || line == null) {
                setText(null);
                setStyle("");
                return;
            }
            setText(line.text().isEmpty() ? " " : line.text());
            setWrapText(wrapLines.isSelected());
            setPrefWidth(wrapLines.isSelected()
                    ? Math.max(100, diffLines.getWidth() - 24)
                    : USE_COMPUTED_SIZE);
            setStyle(styleFor(line.type()));
        }

        private String styleFor(LineType type) {
            String common = "-fx-font-family: Consolas, monospace; "
                    + "-fx-padding: 2 6 2 6;";
            return common + switch (type) {
                case ADDED -> "-fx-background-color: #173b24; -fx-text-fill: #b7f7c8;";
                case REMOVED -> "-fx-background-color: #472020; -fx-text-fill: #ffc1c1;";
                case HUNK_HEADER ->
                        "-fx-background-color: #26384a; -fx-text-fill: #b8d9ff;";
                case FILE_HEADER ->
                        "-fx-background-color: #303030; -fx-text-fill: white; "
                                + "-fx-font-weight: bold;";
                case CONTEXT -> "-fx-background-color: transparent; "
                        + "-fx-text-fill: #dddddd;";
            };
        }
    }

    private record DiffLine(String text, LineType type) {
    }

    private enum LineType {
        ADDED,
        REMOVED,
        HUNK_HEADER,
        FILE_HEADER,
        CONTEXT
    }
}
