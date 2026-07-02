package dev.onion.aicoding.ui;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ReviewPanel extends VBox {

    public enum ReviewStatus {
        IDLE("Idle"),
        WAITING("Waiting"),
        REVIEWING("Reviewing"),
        COMPLETE("Complete"),
        FAILED("Failed");

        private final String label;

        ReviewStatus(String label) {
            this.label = label;
        }
    }

    private final Label statusLabel = new Label();
    private final Label metadataLabel = new Label();
    private final Map<String, TextArea> sections = new LinkedHashMap<>();
    private String rawReview = "";
    private String suggestedPrompt = "";
    private Consumer<String> saveToMemory = review -> { };

    public ReviewPanel() {
        setPrefWidth(360);
        setSpacing(10);
        setStyle("-fx-padding: 12; -fx-background-color: #202020;");

        Label title = new Label("AI Review");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; "
                + "-fx-font-weight: bold;");
        statusLabel.setStyle("-fx-text-fill: #88ff88; -fx-font-weight: bold;");
        metadataLabel.setStyle("-fx-text-fill: #bbbbbb;");
        setStatus(ReviewStatus.IDLE);

        VBox sectionBox = new VBox(10);
        for (String section : java.util.List.of(
                "Summary", "Risks", "Suggestions", "Confidence")) {
            TextArea area = new TextArea();
            area.setEditable(false);
            area.setWrapText(true);
            area.setPrefRowCount(section.equals("Summary") ? 6 : 4);
            sections.put(section, area);
            Label label = new Label(section);
            label.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
            sectionBox.getChildren().addAll(label, area);
        }
        ScrollPane scrollPane = new ScrollPane(sectionBox);
        scrollPane.setFitToWidth(true);

        Button copyReview = new Button("Copy Review");
        copyReview.setOnAction(event -> copy(rawReview));
        Button copyPrompt = new Button("Copy Suggested Prompt");
        copyPrompt.setOnAction(event -> copy(suggestedPrompt));
        Button saveMemory = new Button("Save to Memory");
        saveMemory.setOnAction(event -> {
            if (!rawReview.isBlank()) {
                saveToMemory.accept(rawReview);
            }
        });
        HBox actions = new HBox(8, copyReview, copyPrompt, saveMemory);
        actions.setStyle("-fx-padding: 4 0 0 0;");

        getChildren().addAll(title, statusLabel, metadataLabel, scrollPane, actions);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        setReview("");
    }

    public void setReview(String review) {
        rawReview = review == null ? "" : review;
        Map<String, StringBuilder> parsed = parseSections(rawReview);
        sections.forEach((name, area) -> {
            String text = parsed.get(name).toString().strip();
            area.setText(text.isBlank() ? defaultText(name) : text);
        });
    }

    public void setMetadata(String provider, long elapsedMillis) {
        metadataLabel.setText("Provider: " + provider + "  ·  Elapsed: "
                + elapsedMillis + " ms");
    }

    public void setSuggestedPrompt(String prompt) {
        suggestedPrompt = prompt == null ? "" : prompt;
    }

    public void setOnSaveToMemory(Consumer<String> callback) {
        saveToMemory = callback;
    }

    public void setStatus(ReviewStatus status) {
        statusLabel.setText("Status: " + status.label);
    }

    private Map<String, StringBuilder> parseSections(String review) {
        Map<String, StringBuilder> result = new LinkedHashMap<>();
        sections.keySet().forEach(name -> result.put(name, new StringBuilder()));
        String current = "Summary";
        for (String line : review.split("\\R")) {
            String heading = heading(line);
            if (heading != null) {
                current = heading;
            } else {
                result.get(current).append(line).append('\n');
            }
        }
        if (result.get("Risks").isEmpty()) {
            review.lines().filter(line -> containsAny(line.toLowerCase(Locale.ROOT),
                    "risk", "bug", "security", "failure", "error"))
                    .forEach(line -> result.get("Risks").append(line).append('\n'));
        }
        if (result.get("Suggestions").isEmpty()) {
            review.lines().filter(line -> line.stripLeading().matches("^[-*•].*"))
                    .forEach(line -> result.get("Suggestions").append(line).append('\n'));
        }
        return result;
    }

    private String heading(String line) {
        String normalized = line.replace("#", "").replace("*", "")
                .replace(":", "").strip().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "summary" -> "Summary";
            case "risks", "risk" -> "Risks";
            case "suggestions", "recommendations" -> "Suggestions";
            case "confidence" -> "Confidence";
            default -> null;
        };
    }

    private boolean containsAny(String value, String... terms) {
        return java.util.Arrays.stream(terms).anyMatch(value::contains);
    }

    private String defaultText(String section) {
        return switch (section) {
            case "Summary" -> "No review available.";
            case "Confidence" -> "Not provided.";
            default -> "None identified.";
        };
    }

    private void copy(String text) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
    }
}
