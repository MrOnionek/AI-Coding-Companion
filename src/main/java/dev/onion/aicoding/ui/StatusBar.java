package dev.onion.aicoding.ui;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class StatusBar extends HBox {

    private final Label statusLabel;

    public StatusBar() {
        setStyle("-fx-padding: 8; -fx-background-color: #101010;");

        statusLabel = new Label("No project open");
        statusLabel.setStyle("-fx-text-fill: #88ff88;");

        getChildren().add(statusLabel);
    }

    public void setStatus(String status) {
        statusLabel.setText(status);
    }
}
