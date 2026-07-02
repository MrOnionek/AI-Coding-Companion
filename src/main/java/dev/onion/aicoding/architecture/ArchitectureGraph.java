package dev.onion.aicoding.architecture;

import java.util.List;

public record ArchitectureGraph(
        List<ArchitectureNode> nodes,
        List<ArchitectureEdge> edges) {

    public ArchitectureGraph {
        nodes = List.copyOf(nodes);
        edges = List.copyOf(edges);
    }

    public List<ArchitectureNode> outgoingDependencies(ArchitectureNode node) {
        return edges.stream()
                .filter(edge -> edge.sourceNodeId().equals(node.id()))
                .map(ArchitectureEdge::targetNodeId)
                .distinct()
                .map(this::nodeById)
                .toList();
    }

    public List<ArchitectureNode> incomingReferences(ArchitectureNode node) {
        return edges.stream()
                .filter(edge -> edge.targetNodeId().equals(node.id()))
                .map(ArchitectureEdge::sourceNodeId)
                .distinct()
                .map(this::nodeById)
                .toList();
    }

    private ArchitectureNode nodeById(String id) {
        return nodes.stream().filter(node -> node.id().equals(id)).findFirst()
                .orElseThrow();
    }
}
