package dev.onion.aicoding.ui;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;

public class Toolbar extends MenuBar {

    public Toolbar(Runnable openProject) {
        Menu fileMenu = new Menu("File");
        MenuItem openItem = new MenuItem("Open Project");
        openItem.setOnAction(event -> openProject.run());
        fileMenu.getItems().add(openItem);
        getMenus().add(fileMenu);
    }
}
