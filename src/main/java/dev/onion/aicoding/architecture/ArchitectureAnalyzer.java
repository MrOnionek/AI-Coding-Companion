package dev.onion.aicoding.architecture;

import dev.onion.aicoding.project.Project;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ArchitectureAnalyzer {

    private static final Pattern DECLARATION = Pattern.compile(
            "\\b(class|interface|record|enum)\\s+([A-Za-z_$][\\w$]*)");
    private static final Set<String> IGNORED_DIRECTORIES =
            Set.of(".git", ".gradle", ".idea", "build", "out", "target");

    public ArchitectureGraph analyze(Project project) throws IOException {
        Path root = project.path().toAbsolutePath().normalize();
        List<SourceFile> sources = readSources(root);
        List<ArchitectureNode> nodes = new ArrayList<>();
        for (SourceFile source : sources) {
            Matcher matcher = DECLARATION.matcher(source.content());
            int declarationIndex = 0;
            while (matcher.find()) {
                String name = matcher.group(2);
                nodes.add(new ArchitectureNode(
                        source.path() + "#" + name + "#" + declarationIndex++,
                        name, matcher.group(1), source.path()));
            }
        }

        Map<String, List<ArchitectureNode>> nodesByName = nodes.stream()
                .collect(Collectors.groupingBy(ArchitectureNode::typeName));
        Map<String, SourceFile> sourcesByPath = sources.stream()
                .collect(Collectors.toMap(SourceFile::path, Function.identity()));
        Set<ArchitectureEdge> edges = new LinkedHashSet<>();
        for (ArchitectureNode sourceNode : nodes) {
            String content = sourcesByPath.get(sourceNode.filePath()).content();
            for (Map.Entry<String, List<ArchitectureNode>> entry : nodesByName.entrySet()) {
                Pattern reference = Pattern.compile(
                        "\\b" + Pattern.quote(entry.getKey()) + "\\b");
                if (!reference.matcher(content).find()) {
                    continue;
                }
                for (ArchitectureNode targetNode : entry.getValue()) {
                    if (!sourceNode.id().equals(targetNode.id())) {
                        edges.add(new ArchitectureEdge(sourceNode.id(), targetNode.id()));
                    }
                }
            }
        }
        nodes.sort(Comparator.comparing(ArchitectureNode::typeName)
                .thenComparing(ArchitectureNode::filePath));
        return new ArchitectureGraph(nodes, edges.stream().toList());
    }

    private List<SourceFile> readSources(Path root) throws IOException {
        List<SourceFile> sources = new ArrayList<>();
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory,
                                                     BasicFileAttributes attrs) {
                return !directory.equals(root)
                        && IGNORED_DIRECTORIES.contains(directory.getFileName().toString())
                        ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                if (file.getFileName().toString().endsWith(".java")) {
                    sources.add(new SourceFile(root.relativize(file).toString(),
                            Files.readString(file, StandardCharsets.UTF_8)));
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return sources;
    }

    private record SourceFile(String path, String content) {
    }
}
