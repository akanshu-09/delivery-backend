package org.delivery.deliverybackend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GraphNode implements Comparable<GraphNode> {

    private Node node;
    private double gScore = Double.MAX_VALUE;
    private double fScore = Double.MAX_VALUE;
    private GraphNode parent = null;

    public GraphNode(Node node) {
        this.node = node;
    }

    @Override
    public int compareTo(GraphNode other) {
        return Double.compare(this.fScore, other.fScore);
    }
}
