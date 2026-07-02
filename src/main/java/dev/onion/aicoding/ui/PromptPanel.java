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

        HBox buttons = new HBox(8, copyButton);

        getChildren().addAll(title, promptArea, buttons);
        VBox.setVgrow(promptArea, Priority.ALWAYS);
    }

    public void setPrompt(String prompt) {
        promptArea.setText(prompt);
    }

    private void copyPrompt() {
        ClipboardContent content = new ClipboardContent();
        content.putString(promptArea.getText());
        Clipboard.getSystemClipboard().setContent(content);
    }
}