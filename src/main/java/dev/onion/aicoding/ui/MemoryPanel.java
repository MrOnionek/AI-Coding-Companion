package dev.onion.aicoding.ui;

import dev.onion.aicoding.memory.ProjectMemory;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class MemoryPanel extends VBox {

    private final TextArea memoryArea = new TextArea("No project memory loaded.");

    public MemoryPanel() {
        setPrefWidth(360);
        setSpacing(8);
        setStyle("-fx-padding: 12; -fx-background-color: #202020;");
        Label title = new Label("Project Memory");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; "
                + "-fx-font-weight: bold;");
        memoryArea.setEditable(false);
        memoryArea.setWrapText(true);
        getChildren().addAll(title, memoryArea);
        VBox.setVgrow(memoryArea, Priority.ALWAYS);
    }

    public void setMemory(ProjectMemory memory) {
        memoryArea.setText(memory.toPromptText());
    }

    public void clear() {
        memoryArea.setText("No project memory loaded.");
    }
}
