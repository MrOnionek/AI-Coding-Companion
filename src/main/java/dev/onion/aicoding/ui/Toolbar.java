package dev.onion.aicoding.ui;

import java.util.List;
import java.util.function.Consumer;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;

public class Toolbar extends HBox {

    private final ComboBox<String> providerSelector;

    public Toolbar(Runnable openProject, List<String> providerNames,
                   String activeProvider, Consumer<String> selectProvider,
                   Runnable openSettings) {
        Menu fileMenu = new Menu("File");
        MenuItem openItem = new MenuItem("Open Project");
        openItem.setOnAction(event -> openProject.run());
        MenuItem settingsItem = new MenuItem("Settings");
        settingsItem.setOnAction(event -> openSettings.run());
        fileMenu.getItems().addAll(openItem, settingsItem);
        MenuBar menuBar = new MenuBar(fileMenu);

        providerSelector = new ComboBox<>(
                FXCollections.observableArrayList(providerNames));
        providerSelector.setValue(activeProvider);
        providerSelector.setOnAction(event ->
                selectProvider.accept(providerSelector.getValue()));

        setSpacing(10);
        setStyle("-fx-padding: 4; -fx-alignment: center-left;");
        getChildren().addAll(menuBar, new Label("AI Provider:"), providerSelector);
    }

    public void setSelectedProvider(String providerName) {
        providerSelector.setValue(providerName);
    }
}
