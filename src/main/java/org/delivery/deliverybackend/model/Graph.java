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

    public void updateTrafficFactor(String fromId, String toId, double factor) {
        System.out.println("🚦 Graph: Updating traffic factor for " + fromId + " <-> " + toId + " to " + factor + "x");

        // 1. Update the forward edge (fromId -> toId)
        if (adjacencyList.containsKey(fromId)) {
            for (Edge edge : adjacencyList.get(fromId)) {
                // ✅ FIXED: Using your actual Lombok getter for the string ID
                if (edge.getToNodeId().equals(toId)) {
                    edge.setTrafficWeight(factor);
                    break;
                }
            }
        }

        // 2. Update the reverse edge (toId -> fromId) for bidirectional roads
        if (adjacencyList.containsKey(toId)) {
            for (Edge edge : adjacencyList.get(toId)) {
                // ✅ FIXED: Using your actual Lombok getter for the string ID
                if (edge.getToNodeId().equals(fromId)) {
                    edge.setTrafficWeight(factor);
                    break;
                }
            }
        }
    }}
