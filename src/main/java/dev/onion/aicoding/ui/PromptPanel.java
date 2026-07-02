package dev.onion.aicoding.ui;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class PromptPanel extends VBox {

    private final TextArea promptArea;
    private final Button reviewButton;

    public PromptPanel() {
        setSpacing(8);
        setStyle("-fx-padding: 12; -fx-background-color: #151515;");

        Label title = new Label("Suggested Codex Prompt");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        promptArea = new TextArea();
        promptArea.setPrefRowCount(4);
        promptArea.setWrapText(true);
        promptArea.setStyle("-fx-font-family: Consolas; -fx-font-size: 13px;");

        Button copyButton = new Button("Copy Prompt");
        copyButton.setOnAction(event -> copyPrompt());
        reviewButton = new Button("Request AI Review");

        HBox buttons = new HBox(8, copyButton, reviewButton);

        getChildren().addAll(title, promptArea, buttons);
        VBox.setVgrow(promptArea, Priority.ALWAYS);
    }

    public void setPrompt(String prompt) {
        promptArea.setText(prompt);
    }

    public String getPrompt() {
        return promptArea.getText();
    }

    public void setOnReview(Runnable action) {
        reviewButton.setOnAction(event -> action.run());
    }

    public void setReviewInProgress(boolean inProgress) {
        reviewButton.setDisable(inProgress);
        reviewButton.setText(inProgress ? "Reviewing..." : "Request AI Review");
    }

    private void copyPrompt() {
        ClipboardContent content = new ClipboardContent();
        content.putString(promptArea.getText());
        Clipboard.getSystemClipboard().setContent(content);
    }
}
