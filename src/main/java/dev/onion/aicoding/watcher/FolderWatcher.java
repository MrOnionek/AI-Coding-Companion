package dev.onion.aicoding.watcher;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Consumer;

public class FolderWatcher implements AutoCloseable {

    private final Path projectPath;
    private final WatchService watchService;
    private final Consumer<Path> onJavaFileChanged;

    public FolderWatcher(String projectPath, Consumer<Path> onJavaFileChanged) throws IOException {
        this.projectPath = Paths.get(projectPath);
        this.watchService = FileSystems.getDefault().newWatchService();
        this.onJavaFileChanged = onJavaFileChanged;
    }

    public void startWatching() throws IOException {
        registerAll(projectPath);

        while (true) {
            WatchKey key;

            try {
                key = watchService.take();
            } catch (ClosedWatchServiceException e) {
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            Path directory = (Path) key.watchable();

            for (java.nio.file.WatchEvent<?> event : key.pollEvents()) {
                java.nio.file.WatchEvent.Kind<?> kind = event.kind();

                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }

                Path changed = directory.resolve((Path) event.context());

                if (kind == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(changed)) {
                    registerAll(changed);
                }

                if (shouldIgnore(changed)) {
                    continue;
                }

                onJavaFileChanged.accept(changed);
            }

            if (!key.reset()) {
                break;
            }
        }
    }

    @Override
    public void close() throws IOException {
        watchService.close();
    }

    private void registerAll(Path start) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {

                if (shouldIgnoreDirectory(dir)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                dir.register(
                        watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_DELETE
                );

                return FileVisitResult.CONTINUE;
            }
        });
    }

    private boolean shouldIgnore(Path path) {
        if (Files.isDirectory(path)) {
            return true;
        }

        String text = path.toString().replace("/", "\\");

        return text.contains("\\.git\\")
                || text.contains("\\.idea\\")
                || text.contains("\\.gradle\\")
                || text.contains("\\build\\")
                || text.contains("\\run\\")
                || text.contains("\\out\\")
                || text.contains("\\bin\\")
                || text.endsWith("~")
                || text.endsWith(".class")
                || text.endsWith(".jar")
                || text.endsWith(".log")
                || !text.endsWith(".java");
    }

    private boolean shouldIgnoreDirectory(Path path) {
        String text = path.toString().replace("/", "\\");

        return text.contains("\\.git")
                || text.contains("\\.idea")
                || text.contains("\\.gradle")
                || text.contains("\\build")
                || text.contains("\\run")
                || text.contains("\\out")
                || text.contains("\\bin");
    }
}
