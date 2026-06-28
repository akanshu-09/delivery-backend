package org.delivery.deliverybackend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Graph {
    private List<Node> nodes;
    private List<Edge> edges;
}
