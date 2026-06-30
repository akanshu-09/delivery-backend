package org.delivery.deliverybackend.controller;

import org.delivery.deliverybackend.model.*;
import org.delivery.deliverybackend.repository.AgentRepository;
import org.delivery.deliverybackend.repository.OrderRepository;
import org.delivery.deliverybackend.service.AssignmentAgent;
import org.delivery.deliverybackend.service.GraphLoader;
import org.delivery.deliverybackend.service.RoutingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class OptimizationController {

    private final AssignmentAgent assignmentAgent;
    private final RoutingService routingService;
    private final OrderRepository orderRepository;  // Inject Order Database
    private final AgentRepository agentRepository;  // Inject Agent Database
    private final GraphLoader graphLoader;
    public OptimizationController(AssignmentAgent assignmentAgent,
                                  RoutingService routingService,
                                  OrderRepository orderRepository,
                                  AgentRepository agentRepository, GraphLoader graphLoader) {
        this.assignmentAgent = assignmentAgent;
        this.routingService = routingService;
        this.orderRepository = orderRepository;
        this.agentRepository = agentRepository;
        this.graphLoader = graphLoader;
    }

    @PostMapping("/assignAgent")
    public ResponseEntity<Order> assignAgent(@RequestBody Map<String, String> payload) throws Exception {
        String orderId = payload.get("orderId");
        System.out.println("Running REAL Assignment Agent for order: " + orderId);
        Order updatedOrder = assignmentAgent.assignBestAgent(orderId);
        return ResponseEntity.ok(updatedOrder);
    }

    @PostMapping("/optimizeRoute")
    public ResponseEntity<PathResult> optimizeRoute(@RequestBody Map<String, String> payload)
            throws ExecutionException, InterruptedException {

        String agentId = payload.get("agentId");
        String orderId = payload.get("orderId");

        Agent agent = agentRepository.findById(agentId);
        Order order = orderRepository.findById(orderId);

        if (agent == null || order == null) {
            return ResponseEntity.badRequest().build();
        }

        Node startNode = graphLoader.findNearestNode(agent.getCurrentLocation());
        Node targetNode = graphLoader.findNearestNode(order.getPickupLocation());

        System.out.println("Resolved agent " + agentId + " to node " + startNode.getId());
        System.out.println("Resolved order pickup to node " + targetNode.getId());

        PathResult optimalRoute = routingService.findFastestRoute(
                startNode.getId(), targetNode.getId()
        );

        return ResponseEntity.ok(optimalRoute);
    }

    // ─── REAL TIME DASHBOARD AGGREGATION ENGINE ───
    @GetMapping("/summaryDashboard")
    public ResponseEntity<Map<String, Object>> getSummaryDashboard() throws Exception {
        System.out.println("📊 Aggregating REAL real-time metrics from Firestore...");

        // 1. Fetch fresh data pools from the cloud
        List<Order> allOrders = orderRepository.findAll();
        List<Agent> allAgents = agentRepository.findAll();

        int activeOrdersCount = 0;
        int highRiskCount = 0;
        int availableAgentsCount = 0;
        int busyAgentsCount = 0;

        // 2. Compute live order metrics
        for (Order order : allOrders) {
            // An order is active if it isn't completed or canceled
            if (order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.ASSIGNED) {
                activeOrdersCount++;

                // Temporary simple deadline checker for dashboard speed:
                // If an active order has less than 15 minutes left before deadline, mark as high risk
                long timeLeftMs = order.getDeadlineTimestamp() - System.currentTimeMillis();
                long timeLeftMins = timeLeftMs / (60 * 1000);
                if (timeLeftMins <= 15) {
                    highRiskCount++;
                }
            }
        }

        // 3. Compute live fleet capacity metrics
        for (Agent agent : allAgents) {
            if (agent.getStatus() == AgentStatus.AVAILABLE) {
                availableAgentsCount++;
            } else {
                busyAgentsCount++;
            }
        }

        // 4. Determine overall system operational health
        String systemStatus = "HEALTHY";
        if (highRiskCount > 0 && highRiskCount >= (activeOrdersCount / 2)) {
            systemStatus = "CRITICAL_DELAY";
        } else if (highRiskCount > 0) {
            systemStatus = "WARNING";
        }

        // 5. Package metrics into structured JSON payload
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("availableAgents", availableAgentsCount);
        metrics.put("busyAgents", busyAgentsCount);
        metrics.put("highRiskDeliveries", highRiskCount);
        metrics.put("activeOrders", activeOrdersCount);
        metrics.put("systemStatus", systemStatus);

        return ResponseEntity.ok(metrics);
    }
}