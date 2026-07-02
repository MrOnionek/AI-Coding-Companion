package dev.onion.aicoding.ui;

import dev.onion.aicoding.review.ReviewHistory;
import dev.onion.aicoding.review.ReviewRecord;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ReviewHistoryPanel extends VBox {

    private final TextField searchField = new TextField();
    private final ComboBox<String> providerFilter = new ComboBox<>();
    private final ListView<ReviewRecord> reviewList = new ListView<>();
    private BiFunction<String, String, ReviewHistory> search;

    public ReviewHistoryPanel(List<String> providers) {
        setPrefWidth(360);
        setSpacing(8);
        setStyle("-fx-padding: 12; -fx-background-color: #202020;");
        Label title = new Label("Review History");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; "
                + "-fx-font-weight: bold;");
        searchField.setPromptText("Search reviews");
        providerFilter.setItems(FXCollections.observableArrayList("All providers"));
        providerFilter.getItems().addAll(providers);
        providerFilter.setValue("All providers");
        reviewList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(ReviewRecord review, boolean empty) {
                super.updateItem(review, empty);
                setText(empty || review == null ? null
                        : review.timestamp() + " · " + review.provider()
                        + "\n" + String.join(", ", review.changedFiles()));
            }
        });
        searchField.textProperty().addListener((observable, oldValue, newValue) -> refresh());
        providerFilter.setOnAction(event -> refresh());
        getChildren().addAll(title, searchField, providerFilter, reviewList);
        VBox.setVgrow(reviewList, Priority.ALWAYS);
    }

    public void setSearch(BiFunction<String, String, ReviewHistory> search) {
        this.search = search;
    }

    public void setOnReviewSelected(Consumer<ReviewRecord> callback) {
        reviewList.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, selected) -> {
                    if (selected != null) {
                        callback.accept(selected);
                    }
                });
    }

    public void setHistory(ReviewHistory history) {
        reviewList.setItems(FXCollections.observableArrayList(history.reviews()));
    }

    private void refresh() {
        if (search != null) {
            setHistory(search.apply(searchField.getText(), providerFilter.getValue()));
        }
    }
}
