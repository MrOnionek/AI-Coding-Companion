package dev.onion.aicoding.ui;

import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class DiffViewer extends VBox {

    private final TextArea diffArea;

    public DiffViewer() {
        setSpacing(10);
        setStyle("-fx-padding: 12; -fx-background-color: #181818;");

        Label title = new Label("Git Diff");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        diffArea = new TextArea();
        diffArea.setEditable(false);
        diffArea.setWrapText(false);
        diffArea.setStyle("-fx-font-family: Consolas; -fx-font-size: 13px;");

        getChildren().addAll(title, diffArea);
        VBox.setVgrow(diffArea, Priority.ALWAYS);
    }

    public void setDiff(String diff) {
        if (diff == null || diff.isBlank()) {
            diffArea.setText("Git diff is empty.");
        } else {
            diffArea.setText(diff);
        }
    }
}