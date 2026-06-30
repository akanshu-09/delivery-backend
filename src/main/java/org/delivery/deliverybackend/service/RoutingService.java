package org.delivery.deliverybackend.service;

import org.delivery.deliverybackend.model.*;
import org.delivery.deliverybackend.util.HaversineUtil;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RoutingService {

    private final GraphLoader graphLoader;

    // Inject our in-memory map!
    public RoutingService(GraphLoader graphLoader) {
        this.graphLoader = graphLoader;
    }

    //A* route
    public PathResult findFastestRoute(String startId, String targetId) {
        System.out.println("🗺️ CALCULATING A* ROUTE: " + startId + " ➡️ " + targetId);

        Graph graph = graphLoader.getGraph();
        Map<String, Node> nodes = graph.getNodes();

        // Safety check
        if (!nodes.containsKey(startId) || !nodes.containsKey(targetId)) {
            throw new IllegalArgumentException("Start or Target node does not exist in the map.");
        }

        Node startNode = nodes.get(startId);
        Node targetNode = nodes.get(targetId);

        // A* Initialization: The Priority Queue automatically sorts by lowest fScore
        PriorityQueue<GraphNode> openSet = new PriorityQueue<>();

        // This map keeps track of our calculation state so we don't recalculate nodes
        Map<String, GraphNode> allGraphNodes = new HashMap<>();

        GraphNode startGraphNode = new GraphNode(startNode);
        startGraphNode.setGScore(0.0);
        // The heuristic: True geographic distance to the target
        startGraphNode.setFScore(HaversineUtil.calculateDistance(startNode.getLocation(), targetNode.getLocation()));

        openSet.add(startGraphNode);
        allGraphNodes.put(startId, startGraphNode);

        while (!openSet.isEmpty()) {
            // 1. Grab the most promising node (Lowest fScore)
            GraphNode current = openSet.poll();

            // 2. Did we reach the destination?
            if (current.getNode().getId().equals(targetId)) {
                return reconstructPath(current);
            }

            // 3. Explore all connected roads
            List<Edge> neighbors = graph.getAdjacencyList().getOrDefault(current.getNode().getId(), new ArrayList<>());

            for (Edge edge : neighbors) {
                String neighborId = edge.getToNodeId();
                Node neighborNode = nodes.get(neighborId);

                // Create or fetch the tracking wrapper for this neighbor
                GraphNode neighborGraphNode = allGraphNodes.computeIfAbsent(neighborId, k -> new GraphNode(neighborNode));

                // MATH: Current Distance + (Road Distance * Traffic Multiplier)
                // This is where your custom weight logic perfectly kicks in!
                double tentativeGScore = current.getGScore() + (edge.getDistanceKm() * edge.getTrafficWeight());

                // 4. If we found a FASTER way to reach this neighbor than we previously knew
                if (tentativeGScore < neighborGraphNode.getGScore()) {

                    // Update scores and point the parent backward to trace the route later
                    neighborGraphNode.setParent(current);
                    neighborGraphNode.setGScore(tentativeGScore);

                    // fScore = Exact distance traveled so far + Haversine guess to destination
                    double heuristic = HaversineUtil.calculateDistance(neighborNode.getLocation(), targetNode.getLocation());
                    neighborGraphNode.setFScore(tentativeGScore + heuristic);

                    // Add to queue if it's not already waiting to be explored
                    if (!openSet.contains(neighborGraphNode)) {
                        openSet.add(neighborGraphNode);
                    }
                }
            }
        }

        // If queue empties and we never hit target, the route is impossible
        System.out.println("CRITICAL: No valid route found.");
        return new PathResult(new ArrayList<>(), 0.0);
    }

    public PathResult findDijkstraRoute(String startId, String targetId) {
        System.out.println("🧭 CALCULATING DIJKSTRA ROUTE: " + startId + " ➡️ " + targetId);

        Graph graph = graphLoader.getGraph();
        Map<String, Node> nodes = graph.getNodes();

        PriorityQueue<GraphNode> openSet = new PriorityQueue<>();
        Map<String, GraphNode> allGraphNodes = new HashMap<>();

        GraphNode startGraphNode = new GraphNode(nodes.get(startId));
        startGraphNode.setGScore(0.0);
        startGraphNode.setFScore(0.0); // DIJKSTRA: No Haversine heuristic!

        openSet.add(startGraphNode);
        allGraphNodes.put(startId, startGraphNode);

        while (!openSet.isEmpty()) {
            GraphNode current = openSet.poll();

            if (current.getNode().getId().equals(targetId)) {
                return reconstructPath(current);
            }

            List<Edge> neighbors = graph.getAdjacencyList().getOrDefault(current.getNode().getId(), new ArrayList<>());

            for (Edge edge : neighbors) {
                String neighborId = edge.getToNodeId();
                Node neighborNode = nodes.get(neighborId);

                GraphNode neighborGraphNode = allGraphNodes.computeIfAbsent(neighborId, k -> new GraphNode(neighborNode));
                double tentativeGScore = current.getGScore() + (edge.getDistanceKm() * edge.getTrafficWeight());

                if (tentativeGScore < neighborGraphNode.getGScore()) {
                    neighborGraphNode.setParent(current);
                    neighborGraphNode.setGScore(tentativeGScore);
                    // DIJKSTRA: fScore is ONLY the distance traveled so far.
                    neighborGraphNode.setFScore(tentativeGScore);

                    if (!openSet.contains(neighborGraphNode)) {
                        openSet.add(neighborGraphNode);
                    }
                }
            }
        }
        return new PathResult(new ArrayList<>(), 0.0);
    }

    // Maps raw GPS coordinates to the closest node in our map grid
    public String findNearestNode(double latitude, double longitude) {
        Map<String, Node> nodes = graphLoader.getGraph().getNodes();
        String closestNodeId = null;
        double minDistance = Double.MAX_VALUE;

        // Create a temporary coordinate object for the math
        org.delivery.deliverybackend.model.Location targetLocation =
                new org.delivery.deliverybackend.model.Location(latitude, longitude);

        for (Node node : nodes.values()) {
            double distance = HaversineUtil.calculateDistance(node.getLocation(), targetLocation);
            if (distance < minDistance) {
                minDistance = distance;
                closestNodeId = node.getId();
            }
        }
        return closestNodeId;
    }

    // Helper method to walk backwards from the destination and build the final turn-by-turn list
    private PathResult reconstructPath(GraphNode current) {
        List<String> path = new ArrayList<>();
        double totalCost = current.getGScore();

        while (current != null) {
            path.add(current.getNode().getName());
            current = current.getParent();
        }
        Collections.reverse(path);

        System.out.println("ROUTE FOUND! Path: " + String.join(" ➡️ ", path) + " | Cost: " + String.format("%.2f", totalCost));

        return new PathResult(path, totalCost);
    }
}
