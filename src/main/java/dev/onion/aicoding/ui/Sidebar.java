package dev.onion.aicoding.ui;

import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.nio.file.Path;

public class Sidebar extends VBox {

    private final ListView<String> filesList;
    private final TextArea projectSummary;

    public Sidebar() {
        setPrefWidth(280);
        setSpacing(10);
        setStyle("-fx-padding: 12; -fx-background-color: #202020;");

        Label title = new Label("Changed Files");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        filesList = new ListView<>();
        projectSummary = new TextArea("No project indexed.");
        projectSummary.setEditable(false);
        projectSummary.setWrapText(true);
        projectSummary.setPrefRowCount(10);

        Label summaryTitle = new Label("Project Summary");
        summaryTitle.setStyle("-fx-text-fill: white; -fx-font-size: 16px; "
                + "-fx-font-weight: bold;");

        getChildren().addAll(title, filesList, summaryTitle, projectSummary);
    }

    public void addChangedFile(Path file) {
        String fileName = file.getFileName().toString();

        if (!filesList.getItems().contains(fileName)) {
            filesList.getItems().add(fileName);
        }
    }

    public void setProjectSummary(dev.onion.aicoding.project.ProjectSummary summary) {
        var info = summary.projectInfo();
        String packages = summary.packageNames().isEmpty()
                ? "(none)" : String.join("\n", summary.packageNames());
        projectSummary.setText("""
                Name: %s
                Path: %s
                Git repository: %s
                Java files: %d
                Packages: %d
                Build system: %s
                Language: %s

                Package names:
                %s
                """.formatted(
                info.name(), info.absolutePath(), info.gitRepository() ? "Yes" : "No",
                summary.totalJavaFiles(), summary.totalPackages(), info.buildSystem(),
                info.language(), packages));
    }

    public void setProjectAnalysis(dev.onion.aicoding.project.ProjectAnalysis analysis) {
        var summary = analysis.summary();
        var info = summary.projectInfo();
        String entries = analysis.entryPoints().isEmpty()
                ? "(none)" : String.join("\n", analysis.entryPoints());
        String largest = analysis.largestJavaFiles().stream()
                .map(file -> file.path() + " (" + file.lines() + " lines)")
                .collect(java.util.stream.Collectors.joining("\n"));
        String important = analysis.importantClasses().stream()
                .map(type -> type.name() + " [" + String.join(", ", type.reasons())
                        + "; references: " + type.referencedByFiles() + "]")
                .collect(java.util.stream.Collectors.joining("\n"));
        projectSummary.setText("""
                Name: %s
                Path: %s
                Git repository: %s
                Build / frameworks: %s
                Java files: %d
                Packages: %d
                Classes: %d | Interfaces: %d
                Records: %d | Enums: %d

                Entry points:
                %s

                Important classes:
                %s

                Largest Java files:
                %s
                """.formatted(info.name(), info.absolutePath(),
                info.gitRepository() ? "Yes" : "No",
                analysis.detectedTechnologies().isEmpty()
                        ? "Unknown" : String.join(", ", analysis.detectedTechnologies()),
                summary.totalJavaFiles(), summary.totalPackages(),
                analysis.declarationCounts().getOrDefault("classes", 0),
                analysis.declarationCounts().getOrDefault("interfaces", 0),
                analysis.declarationCounts().getOrDefault("records", 0),
                analysis.declarationCounts().getOrDefault("enums", 0),
                entries, important.isBlank() ? "(none)" : important,
                largest.isBlank() ? "(none)" : largest));
    }

    public void clearProjectSummary() {
        projectSummary.setText("No project indexed.");
    }
}
