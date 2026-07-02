package dev.onion.aicoding.project;

import dev.onion.aicoding.model.ProjectInfo;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProjectAnalyzer {

    private static final Pattern PACKAGE =
            Pattern.compile("(?m)^\\s*package\\s+([\\w.]+)\\s*;");
    private static final Pattern DECLARATION = Pattern.compile(
            "\\b(class|interface|record|enum)\\s+([A-Za-z_$][\\w$]*)");
    private static final Pattern MAIN = Pattern.compile(
            "\\bpublic\\s+static\\s+void\\s+main\\s*\\(");
    private static final Set<String> IGNORED =
            Set.of(".git", ".gradle", ".idea", "build", "out", "target");

    public ProjectAnalysis analyze(Project project) throws IOException {
        Path root = project.path().toAbsolutePath().normalize();
        List<SourceFile> sources = readSources(root);
        Set<String> packages = new TreeSet<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("classes", 0);
        counts.put("interfaces", 0);
        counts.put("records", 0);
        counts.put("enums", 0);
        List<TypeDeclaration> declarations = new ArrayList<>();
        Set<String> entryPoints = new LinkedHashSet<>();

        for (SourceFile source : sources) {
            Matcher packageMatcher = PACKAGE.matcher(source.content());
            if (packageMatcher.find()) {
                packages.add(packageMatcher.group(1));
            }
            Matcher declarationMatcher = DECLARATION.matcher(source.content());
            while (declarationMatcher.find()) {
                String kind = declarationMatcher.group(1);
                counts.compute(kind + (kind.equals("class") ? "es" : "s"),
                        (key, value) -> value + 1);
                declarations.add(new TypeDeclaration(
                        declarationMatcher.group(2), source.relativePath(), source));
            }
            boolean serverInitializer = source.content().contains("ModInitializer");
            boolean clientInitializer = source.content().contains("ClientModInitializer");
            boolean main = MAIN.matcher(source.content()).find();
            if (serverInitializer || clientInitializer || main) {
                String type = primaryType(source, declarations);
                if (serverInitializer) {
                    entryPoints.add(type + " (ModInitializer)");
                }
                if (clientInitializer) {
                    entryPoints.add(type + " (ClientModInitializer)");
                }
                if (main) {
                    entryPoints.add(type + " (main)");
                }
            }
        }

        String buildSystem = detectBuildSystem(root);
        ProjectSummary summary = new ProjectSummary(new ProjectInfo(
                root.getFileName() == null ? root.toString() : root.getFileName().toString(),
                root.toString(), project.gitRepository(), buildSystem, "Java"),
                sources.size(), packages.size(), packages.stream().toList());
        List<ProjectAnalysis.FileRank> largest = sources.stream()
                .sorted(Comparator.comparingInt(SourceFile::lines).reversed())
                .limit(10)
                .map(source -> new ProjectAnalysis.FileRank(
                        source.relativePath(), source.lines()))
                .toList();

        Map<TypeDeclaration, Integer> references = referenceCounts(declarations, sources);
        List<ProjectAnalysis.ImportantClass> important =
                importantClasses(declarations, references, largest, entryPoints);
        return new ProjectAnalysis(summary, counts, detectTechnologies(root, sources,
                buildSystem), entryPoints.stream().toList(), largest, important);
    }

    private List<SourceFile> readSources(Path root) throws IOException {
        List<SourceFile> sources = new ArrayList<>();
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                return !dir.equals(root) && IGNORED.contains(dir.getFileName().toString())
                        ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                if (file.getFileName().toString().endsWith(".java")) {
                    String content = Files.readString(file, StandardCharsets.UTF_8);
                    int lines = content.isEmpty() ? 0 : content.split("\\R", -1).length;
                    sources.add(new SourceFile(root.relativize(file).toString(), content, lines));
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return sources;
    }

    private Map<TypeDeclaration, Integer> referenceCounts(
            List<TypeDeclaration> declarations, List<SourceFile> sources) {
        Map<TypeDeclaration, Integer> result = new LinkedHashMap<>();
        for (TypeDeclaration declaration : declarations) {
            Pattern reference = Pattern.compile("\\b" + Pattern.quote(declaration.name()) + "\\b");
            int count = (int) sources.stream()
                    .filter(source -> source != declaration.source())
                    .filter(source -> reference.matcher(source.content()).find())
                    .count();
            result.put(declaration, count);
        }
        return result;
    }

    private List<ProjectAnalysis.ImportantClass> importantClasses(
            List<TypeDeclaration> declarations,
            Map<TypeDeclaration, Integer> references,
            List<ProjectAnalysis.FileRank> largest,
            Set<String> entryPoints) {
        Map<TypeDeclaration, LinkedHashSet<String>> selected = new LinkedHashMap<>();
        references.entrySet().stream()
                .sorted(Map.Entry.<TypeDeclaration, Integer>comparingByValue().reversed())
                .filter(entry -> entry.getValue() > 0).limit(5)
                .forEach(entry -> selected.computeIfAbsent(entry.getKey(),
                        key -> new LinkedHashSet<>()).add("referenced by many files"));
        Set<String> largestPaths = largest.stream().limit(5)
                .map(ProjectAnalysis.FileRank::path).collect(java.util.stream.Collectors.toSet());
        for (TypeDeclaration declaration : declarations) {
            if (largestPaths.contains(declaration.path())) {
                selected.computeIfAbsent(declaration, key -> new LinkedHashSet<>())
                        .add("large source file");
            }
            if (entryPoints.stream().anyMatch(entry -> entry.startsWith(declaration.name() + " "))) {
                selected.computeIfAbsent(declaration, key -> new LinkedHashSet<>())
                        .add("entry point");
            }
        }
        return selected.entrySet().stream()
                .map(entry -> new ProjectAnalysis.ImportantClass(entry.getKey().name(),
                        entry.getKey().path(), references.getOrDefault(entry.getKey(), 0),
                        entry.getValue().stream().toList()))
                .toList();
    }

    private List<String> detectTechnologies(
            Path root, List<SourceFile> sources, String buildSystem) throws IOException {
        Set<String> detected = new LinkedHashSet<>();
        if (!buildSystem.equals("Unknown")) {
            detected.add(buildSystem);
        }
        String sourceText = sources.stream().map(SourceFile::content)
                .collect(java.util.stream.Collectors.joining("\n"));
        String buildText = readBuildFiles(root);
        String all = sourceText + "\n" + buildText;
        if (Files.exists(root.resolve("fabric.mod.json"))
                || all.contains("net.fabricmc") || all.contains("ModInitializer")) {
            detected.add("Fabric");
        }
        if (Files.exists(root.resolve("src/main/resources/META-INF/mods.toml"))
                || all.contains("net.minecraftforge") || all.contains("@Mod(")) {
            detected.add("Forge");
        }
        if (all.contains("org.springframework") || all.contains("spring-boot")
                || all.contains("@SpringBootApplication")) {
            detected.add("Spring");
        }
        return detected.stream().toList();
    }

    private String readBuildFiles(Path root) throws IOException {
        StringBuilder text = new StringBuilder();
        for (String name : List.of("build.gradle", "build.gradle.kts", "settings.gradle",
                "settings.gradle.kts", "pom.xml")) {
            Path file = root.resolve(name);
            if (Files.isRegularFile(file)) {
                text.append(Files.readString(file, StandardCharsets.UTF_8)).append('\n');
            }
        }
        return text.toString();
    }

    private String detectBuildSystem(Path root) {
        if (exists(root, "build.gradle") || exists(root, "build.gradle.kts")
                || exists(root, "settings.gradle") || exists(root, "settings.gradle.kts")) {
            return "Gradle";
        }
        return exists(root, "pom.xml") ? "Maven" : "Unknown";
    }

    private boolean exists(Path root, String name) {
        return Files.isRegularFile(root.resolve(name));
    }

    private String primaryType(SourceFile source, List<TypeDeclaration> declarations) {
        return declarations.stream().filter(type -> type.source() == source)
                .map(TypeDeclaration::name).findFirst()
                .orElse(source.relativePath().replaceFirst("\\.java$", ""));
    }

    private record SourceFile(String relativePath, String content, int lines) {
    }

    private record TypeDeclaration(String name, String path, SourceFile source) {
    }
}
