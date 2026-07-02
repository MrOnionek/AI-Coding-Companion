package dev.onion.aicoding.ui;

import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ReviewPanel extends VBox {

    private final TextArea reviewArea;

    public ReviewPanel() {
        setPrefWidth(360);
        setSpacing(10);
        setStyle("-fx-padding: 12; -fx-background-color: #202020;");

        Label title = new Label("ChatGPT Review");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        reviewArea = new TextArea();
        reviewArea.setEditable(false);
        reviewArea.setWrapText(true);
        reviewArea.setStyle("-fx-font-family: Consolas; -fx-font-size: 13px;");

        getChildren().addAll(title, reviewArea);
        VBox.setVgrow(reviewArea, Priority.ALWAYS);
    }

    public void setReview(String review) {
        reviewArea.setText(review);
    }
}