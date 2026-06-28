package org.delivery.deliverybackend.controller;

import org.delivery.deliverybackend.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // CORS
public class OptimizationController {

    @PostMapping("/assignAgent")
    public ResponseEntity<Order> assignAgent(@RequestBody Map<String, String> payload) {
        String orderId = payload.get("orderId");
        System.out.println("Stub: Running Assignment Agent for order " + orderId);

        Order updatedOrder = new Order(
                orderId != null ? orderId : "ORD-1001",
                new Location(28.6139, 77.2090), // Connaught Place
                new Location(28.5355, 77.3910), // Noida
                OrderStatus.ASSIGNED,
                System.currentTimeMillis() + 3600000,
                1,
                "agent-001"
        );
        return ResponseEntity.ok(updatedOrder);
    }

    @PostMapping("/optimizeRoute")
    public ResponseEntity<Route> optimizeRoute(@RequestBody Map<String, String> payload) {
        String agentId = payload.get("agentId");
        System.out.println("Stub: Calculating optimal route for agent " + agentId);

        Route fakeRoute = new Route(
                "RT-999",
                agentId != null ? agentId : "agent-001",
                Arrays.asList("ORD-1001", "ORD-1002"),
                Arrays.asList(new Location(28.6139, 77.2090), new Location(28.5355, 77.3910)),
                45,
                RouteStatus.ACTIVE,
                System.currentTimeMillis(),
                15.2,
                System.currentTimeMillis() + 2700000
        );
        return ResponseEntity.ok(fakeRoute);
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
