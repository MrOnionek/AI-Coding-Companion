package dev.onion.aicoding.ui;

import java.nio.file.Path;
import java.util.Optional;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

public class ProjectChooser {

    public Optional<Path> choose(Window owner) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Open Project");
        return Optional.ofNullable(chooser.showDialog(owner)).map(file -> file.toPath());
    }
}
