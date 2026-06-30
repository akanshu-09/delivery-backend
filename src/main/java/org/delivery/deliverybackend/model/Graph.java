package org.delivery.deliverybackend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Graph {
    private Map<String, Node> nodes = new HashMap<>();
    private Map<String, List<Edge>> adjacencyList = new HashMap<>();
}
