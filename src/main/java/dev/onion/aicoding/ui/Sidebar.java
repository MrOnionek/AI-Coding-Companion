package dev.onion.aicoding.ui;

import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;

import java.nio.file.Path;

public class Sidebar extends VBox {

    private final ListView<String> filesList;

    public Sidebar() {
        setPrefWidth(280);
        setSpacing(10);
        setStyle("-fx-padding: 12; -fx-background-color: #202020;");

        Label title = new Label("Changed Files");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        filesList = new ListView<>();

        getChildren().addAll(title, filesList);
    }

    public void addChangedFile(Path file) {
        String fileName = file.getFileName().toString();

        if (!filesList.getItems().contains(fileName)) {
            filesList.getItems().add(fileName);
        }
    }
}