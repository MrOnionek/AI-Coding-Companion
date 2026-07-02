package dev.onion.aicoding.search;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProjectSearch implements AutoCloseable {

    private static final Pattern TYPE = Pattern.compile(
            "\\b(class|interface|record|enum)\\s+([A-Za-z_$][\\w$]*)");
    private static final Pattern METHOD = Pattern.compile(
            "(?m)^\\s*(?:(?:public|protected|private|static|final|synchronized|abstract|"
                    + "native|default|strictfp)\\s+)*"
                    + "[A-Za-z_$][\\w$<>\\[\\],.? ]*\\s+"
                    + "([A-Za-z_$][\\w$]*)\\s*\\(");
    private static final Pattern PACKAGE =
            Pattern.compile("(?m)^\\s*package\\s+([\\w.]+)\\s*;");
    private static final Set<String> IGNORED =
            Set.of(".git", ".gradle", ".idea", "build", "out", "target");
    private final ExecutorService indexer = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "project-search-indexer");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicLong generation = new AtomicLong();
    private volatile Path projectPath;
    private volatile SearchIndex index = SearchIndex.empty();
    private Consumer<SearchIndex> onIndexUpdated = value -> { };

    public void openProject(Path path) {
        projectPath = path.toAbsolutePath().normalize();
        rebuild();
    }

    public void fileChanged(Path ignoredChangedFile) {
        rebuild();
    }

    public List<SearchResult> search(String query) {
        String normalized = normalize(query);
        if (normalized.isBlank()) {
            return List.of();
        }
        return index.entries().stream()
                .map(result -> result.withScore(score(normalized, result)))
                .filter(result -> result.score() > 0)
                .sorted(Comparator.comparingInt(SearchResult::score).reversed()
                        .thenComparing(SearchResult::name))
                .limit(100)
                .toList();
    }

    public void onIndexUpdated(Consumer<SearchIndex> callback) {
        onIndexUpdated = callback;
    }

    private void rebuild() {
        Path root = projectPath;
        if (root == null) {
            return;
        }
        long version = generation.incrementAndGet();
        indexer.submit(() -> {
            try {
                SearchIndex rebuilt = buildIndex(root);
                if (version == generation.get() && root.equals(projectPath)) {
                    index = rebuilt;
                    onIndexUpdated.accept(rebuilt);
                }
            } catch (IOException ignored) {
                // Search indexing is best-effort and never blocks project work.
            }
        });
    }

    private SearchIndex buildIndex(Path root) throws IOException {
        List<SearchResult> entries = new ArrayList<>();
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory,
                                                     BasicFileAttributes attrs) {
                return !directory.equals(root)
                        && IGNORED.contains(directory.getFileName().toString())
                        ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                if (file.getFileName().toString().endsWith(".java")) {
                    indexJavaFile(file, entries);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return new SearchIndex(entries);
    }

    private void indexJavaFile(Path file, List<SearchResult> entries) throws IOException {
        String content = Files.readString(file, StandardCharsets.UTF_8);
        entries.add(new SearchResult(file.getFileName().toString(),
                SearchResult.Kind.FILE, file, 1, 0));
        Matcher packageMatcher = PACKAGE.matcher(content);
        if (packageMatcher.find()) {
            entries.add(result(packageMatcher.group(1), SearchResult.Kind.PACKAGE,
                    file, content, packageMatcher.start(1)));
        }
        Matcher typeMatcher = TYPE.matcher(content);
        while (typeMatcher.find()) {
            entries.add(result(typeMatcher.group(2),
                    SearchResult.Kind.valueOf(typeMatcher.group(1)
                            .toUpperCase(Locale.ROOT)),
                    file, content, typeMatcher.start(2)));
        }
        Matcher methodMatcher = METHOD.matcher(content);
        while (methodMatcher.find()) {
            String name = methodMatcher.group(1);
            if (!Set.of("if", "for", "while", "switch", "catch", "new")
                    .contains(name)) {
                entries.add(result(name, SearchResult.Kind.METHOD,
                        file, content, methodMatcher.start(1)));
            }
        }
    }

    private SearchResult result(String name, SearchResult.Kind kind, Path file,
                                String content, int offset) {
        int line = 1;
        for (int index = 0; index < offset; index++) {
            if (content.charAt(index) == '\n') {
                line++;
            }
        }
        return new SearchResult(name, kind, file, line, 0);
    }

    private int score(String query, SearchResult result) {
        String candidate = normalize(result.name());
        int match;
        if (candidate.equals(query)) {
            match = 1000;
        } else if (candidate.startsWith(query)) {
            match = 700;
        } else if (candidate.contains(query)) {
            match = 500;
        } else {
            int fuzzy = fuzzyScore(query, candidate);
            if (fuzzy == 0) {
                return 0;
            }
            match = 200 + fuzzy;
        }
        return match + switch (result.kind()) {
            case CLASS, INTERFACE, RECORD, ENUM -> 300;
            case FILE -> 200;
            case METHOD -> 100;
            case PACKAGE -> 50;
        };
    }

    private int fuzzyScore(String query, String candidate) {
        int queryIndex = 0;
        int score = 0;
        int lastMatch = -2;
        for (int index = 0; index < candidate.length() && queryIndex < query.length(); index++) {
            if (candidate.charAt(index) == query.charAt(queryIndex)) {
                score += index == lastMatch + 1 ? 8 : 3;
                lastMatch = index;
                queryIndex++;
            }
        }
        return queryIndex == query.length() ? score : 0;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }

    @Override
    public void close() {
        generation.incrementAndGet();
        indexer.shutdownNow();
    }
}
