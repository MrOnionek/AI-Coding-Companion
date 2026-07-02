package dev.onion.aicoding.project;

import java.nio.file.Files;
import java.nio.file.Path;

public record Project(Path path, boolean gitRepository) {

    public Project(Path path) {
        this(path.toAbsolutePath().normalize(), Files.isDirectory(path.resolve(".git")));
    }
}
