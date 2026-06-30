package org.delivery.deliverybackend.controller;

import org.delivery.deliverybackend.model.*;
import org.delivery.deliverybackend.service.AssignmentAgent;
import org.delivery.deliverybackend.service.RoutingService; // Import your new A* Engine!
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class OptimizationController {

    private final AssignmentAgent assignmentAgent;
    private final RoutingService routingService; // 1. Add the Routing Service

    // 2. Inject it via Constructor
    public OptimizationController(AssignmentAgent assignmentAgent, RoutingService routingService) {
        this.assignmentAgent = assignmentAgent;
        this.routingService = routingService;
    }

    // (Keep your existing assignAgent method here...)
    @PostMapping("/assignAgent")
    public ResponseEntity<Order> assignAgent(@RequestBody Map<String, String> payload) throws Exception {
        String orderId = payload.get("orderId");
        System.out.println("Running REAL Assignment Agent for order: " + orderId);
        Order updatedOrder = assignmentAgent.assignBestAgent(orderId);
        return ResponseEntity.ok(updatedOrder);
    }

    // 3. THE REAL A* ROUTING ENDPOINT
    @PostMapping("/optimizeRoute")
    public ResponseEntity<List<String>> optimizeRoute(@RequestBody Map<String, String> payload) {
        String startId = payload.get("startNodeId");
        String targetId = payload.get("targetNodeId");

        System.out.println("Received routing request from " + startId + " to " + targetId);

        // Call the Head Chef to run the math!
        List<String> optimalPath = routingService.findFastestRoute(startId, targetId);

        return ResponseEntity.ok(optimalPath);
    }


    @PostMapping("/recalculateRoute")
    public ResponseEntity<Route> recalculateRoute(@RequestBody Map<String, String> payload) {
        System.out.println("Stub: Recalculating route due to system change");

        Route fakeRoute = new Route(
                "RT-999-RECALC",
                "agent-001",
                Arrays.asList("ORD-1001"),
                Arrays.asList(new Location(28.6139, 77.2090), new Location(28.5355, 77.3910)),
                30,
                RouteStatus.ACTIVE,
                System.currentTimeMillis(),
                10.5,
                System.currentTimeMillis() + 1800000
        );
        return ResponseEntity.ok(fakeRoute);
    }

    @GetMapping("/route/{agentId}")
    public ResponseEntity<Route> getRouteForAgent(@PathVariable String agentId) {
        System.out.println("Stub: Fetching active route for agent " + agentId);

        Route fakeRoute = new Route(
                "RT-888",
                agentId,
                Arrays.asList("ORD-1005"),
                Arrays.asList(new Location(28.6514, 77.1907), new Location(28.4595, 77.0266)),
                60,
                RouteStatus.ACTIVE,
                System.currentTimeMillis(),
                25.0,
                System.currentTimeMillis() + 3600000
        );
        return ResponseEntity.ok(fakeRoute);
    }

    @GetMapping("/dashboard/summary")
    public ResponseEntity<Map<String, Object>> getDashboardSummary() {
        System.out.println("Stub: Fetching dashboard summary metrics");

        return ResponseEntity.ok(Map.of(
                "activeOrders", 14,
                "availableAgents", 3,
                "busyAgents", 8,
                "highRiskDeliveries", 2,
                "systemStatus", "HEALTHY"
        ));
    }
}
