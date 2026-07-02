package dev.onion.aicoding.ui;

import dev.onion.aicoding.task.TaskItem;
import dev.onion.aicoding.task.TaskPlan;
import dev.onion.aicoding.task.TaskStatus;
import java.util.function.BiConsumer;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class TaskPanel extends VBox {

    private final ListView<TaskItem> taskList = new ListView<>();
    private BiConsumer<String, TaskStatus> updateStatus;

    public TaskPanel() {
        setPrefWidth(360);
        setSpacing(8);
        setStyle("-fx-padding: 12; -fx-background-color: #202020;");
        Label title = new Label("Tasks");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; "
                + "-fx-font-weight: bold;");
        taskList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(TaskItem task, boolean empty) {
                super.updateItem(task, empty);
                setText(empty || task == null ? null
                        : "[" + task.priority() + "] [" + task.status() + "] "
                        + task.title() + "\n" + String.join(", ", task.affectedFiles()));
            }
        });
        Button done = new Button("Mark Done");
        done.setOnAction(event -> updateSelected(TaskStatus.DONE));
        Button ignored = new Button("Ignore");
        ignored.setOnAction(event -> updateSelected(TaskStatus.IGNORED));
        getChildren().addAll(title, taskList, new HBox(8, done, ignored));
        VBox.setVgrow(taskList, Priority.ALWAYS);
    }

    public void setTasks(TaskPlan plan) {
        taskList.setItems(FXCollections.observableArrayList(plan.tasks()));
    }

    public void setOnStatusChanged(BiConsumer<String, TaskStatus> callback) {
        updateStatus = callback;
    }

    private void updateSelected(TaskStatus status) {
        TaskItem selected = taskList.getSelectionModel().getSelectedItem();
        if (selected != null && updateStatus != null) {
            updateStatus.accept(selected.id(), status);
        }
    }
}
