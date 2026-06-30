package org.delivery.deliverybackend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.delivery.deliverybackend.model.Edge;
import org.delivery.deliverybackend.model.Graph;
import org.delivery.deliverybackend.model.Location;
import org.delivery.deliverybackend.model.Node;
import org.delivery.deliverybackend.util.HaversineUtil;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GraphLoader implements CommandLineRunner {

    private final Graph systemGraph = new Graph();

    // CommandLineRunner forces Spring Boot to execute this method immediately after startup
    @Override
    public void run(String... args) {
        try {
            System.out.println("====== SYSTEM INITIALIZATION: LOADING GRAPH MAP ======");

            ObjectMapper mapper = new ObjectMapper();
            InputStream inputStream = new ClassPathResource("graph-seed.json").getInputStream();

            Map<String, Object> rawData = mapper.readValue(inputStream, new TypeReference<Map<String, Object>>() {});

            // 1. Parse and map Nodes
            List<Node> nodes = mapper.convertValue(rawData.get("nodes"), new TypeReference<List<Node>>() {});
            Map<String, Node> nodeMap = new HashMap<>();

            for (Node node : nodes) {
                nodeMap.put(node.getId(), node);
                // Initialize the empty list for this node's road connections
                systemGraph.getAdjacencyList().put(node.getId(), new java.util.ArrayList<>());
                System.out.println("Registered City Node: " + node.getName());
            }
            systemGraph.setNodes(nodeMap);

            // 2. Parse and map Edges (Bidirectional Roads)
            List<Map<String, Object>> edgesData = mapper.convertValue(rawData.get("edges"), new TypeReference<List<Map<String, Object>>>() {});

            for (Map<String, Object> edgeData : edgesData) {
                String from = (String) edgeData.get("fromNodeId");
                String to = (String) edgeData.get("toNodeId");
                double distance = ((Number) edgeData.get("distanceKm")).doubleValue();
                double traffic = ((Number) edgeData.get("trafficWeight")).doubleValue();

                // Forward connection
                Edge edge = new Edge(from, to, distance, traffic);
                systemGraph.getAdjacencyList().get(from).add(edge);

                // Reverse connection (so drivers can drive both ways)
                Edge reverseEdge = new Edge(to, from, distance, traffic);
                systemGraph.getAdjacencyList().get(to).add(reverseEdge);
            }

            System.out.println("====== GRAPH LOADING COMPLETE: " + nodes.size() + " NODES LIVE IN MEMORY ======");

        } catch (Exception e) {
            System.err.println("CRITICAL: Failed to load graph-seed.json on startup!");
            e.printStackTrace();
        }
    }

    public Graph getGraph() {
        return this.systemGraph;
    }

    public Node findNearestNode(Location location) {
        Node nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (Node node : systemGraph.getNodes().values()) {
            double distance = HaversineUtil.calculateDistance(location, node.getLocation());
            if (distance < minDistance) {
                minDistance = distance;
                nearest = node;
            }
        }

        return nearest;
    }
}
