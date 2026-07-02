package dev.onion.aicoding.ui;

import dev.onion.aicoding.architecture.ArchitectureGraph;
import dev.onion.aicoding.architecture.ArchitectureNode;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ArchitecturePanel extends VBox {

    private final ListView<ArchitectureNode> nodeList = new ListView<>();
    private final TextArea details = new TextArea("Select a type to inspect its dependencies.");
    private ArchitectureGraph graph = new ArchitectureGraph(
            java.util.List.of(), java.util.List.of());

    public ArchitecturePanel() {
        setPrefWidth(360);
        setSpacing(8);
        setStyle("-fx-padding: 12; -fx-background-color: #202020;");
        Label title = new Label("Architecture");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; "
                + "-fx-font-weight: bold;");
        nodeList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(ArchitectureNode node, boolean empty) {
                super.updateItem(node, empty);
                setText(empty || node == null ? null
                        : node.typeName() + " · " + node.typeKind());
            }
        });
        nodeList.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, node) -> showNode(node));
        details.setEditable(false);
        details.setWrapText(true);
        SplitPane content = new SplitPane(nodeList, details);
        content.setDividerPositions(0.42);
        getChildren().addAll(title, content);
        VBox.setVgrow(content, Priority.ALWAYS);
    }

    public void setGraph(ArchitectureGraph graph) {
        this.graph = graph;
        nodeList.setItems(FXCollections.observableArrayList(graph.nodes()));
        details.setText(graph.nodes().isEmpty()
                ? "No Java types found." : "Select a type to inspect its dependencies.");
    }

    public void clear() {
        setGraph(new ArchitectureGraph(java.util.List.of(), java.util.List.of()));
    }

    private void showNode(ArchitectureNode node) {
        if (node == null) {
            return;
        }
        String outgoing = names(graph.outgoingDependencies(node));
        String incoming = names(graph.incomingReferences(node));
        details.setText("""
                Type: %s
                Kind: %s
                File: %s

                Outgoing dependencies:
                %s

                Incoming references:
                %s
                """.formatted(node.typeName(), node.typeKind(), node.filePath(),
                outgoing, incoming));
    }

    private String names(java.util.List<ArchitectureNode> nodes) {
        return nodes.isEmpty() ? "(none)" : nodes.stream()
                .map(node -> node.typeName() + " — " + node.filePath())
                .collect(java.util.stream.Collectors.joining("\n"));
    }
}
