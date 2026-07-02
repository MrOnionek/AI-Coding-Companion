package dev.onion.aicoding.app;

import dev.onion.aicoding.ui.MainWindow;
import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {

    private AppContext context;
    private MainWindow mainWindow;

    @Override
    public void start(Stage stage) {
        context = new AppContext();
        mainWindow = new MainWindow(context);
        mainWindow.show(stage);
    }

    @Override
    public void stop() {
        if (mainWindow != null) {
            mainWindow.close();
        }
        if (context != null) {
            context.projectManager().closeProject();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
