package org.delivery.deliverybackend.service;

import com.google.cloud.firestore.DocumentChange;
import com.google.cloud.firestore.Firestore;
import org.delivery.deliverybackend.model.Order;
import org.delivery.deliverybackend.model.OrderStatus;
import org.delivery.deliverybackend.model.PathResult;
import org.delivery.deliverybackend.repository.OrderRepository;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct; // Use javax.annotation.PostConstruct if on older Spring Boot/Java

import java.util.List;

@Component
public class MonitoringAgent {

    private final Firestore db;
    private final AssignmentAgent assignmentAgent;
    private final RouteOptimizationAgent routeOptimizationAgent;
    private final OrderRepository orderRepository;

    // Injecting the core logic engines
    public MonitoringAgent(Firestore db, AssignmentAgent assignmentAgent, RouteOptimizationAgent routeOptimizationAgent,OrderRepository orderRepository) {
        this.db = db;
        this.assignmentAgent = assignmentAgent;
        this.routeOptimizationAgent = routeOptimizationAgent;
        this.orderRepository = orderRepository;
    }

    @PostConstruct
    public void startListeners() {
        System.out.println("🎧 MonitoringAgent: Booting up live Firestore listeners...");

        // 1. The PENDING Order Watcher
        // 2. The ALL-SEEING Agent Watcher (Debug Mode)
        // 2. The OFFLINE Agent Watcher (Clean Mode)
        db.collection("orders")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) return;

                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            // We watch for ADDED (new orders) AND MODIFIED (if an order was updated to PENDING)
                            if (dc.getType() == DocumentChange.Type.ADDED || dc.getType() == DocumentChange.Type.MODIFIED) {
                                Order order = dc.getDocument().toObject(Order.class);

                                // FILTER IN JAVA: Check status here
                                // Note: Use .toString() if comparing against a String "PENDING"
                                if ("PENDING".equals(order.getStatus().toString())) {
                                    System.out.println("🚨 LIVE EVENT: Detected PENDING order -> " + order.getId());
                                    onNewOrder(order);
                                }
                            }
                        }
                    }
                });
    }

    // --- THE BRAIN (Checkpoint 3) ---
    private void onNewOrder(Order newOrder) {
        // Wrapped in a try/catch so if the math fails, it doesn't kill our background listener thread!
        try {
            System.out.println("⚙️ Processing autonomous assignment for: " + newOrder.getId());

            // 1. Assign the best driver
            Order assignedOrder = assignmentAgent.assignBestAgent(newOrder.getId());

            // 2. If assignment succeeded, instantly calculate their new A* route
            if (assignedOrder != null && assignedOrder.getAssignedAgentId() != null) {
                String agentId = assignedOrder.getAssignedAgentId();
                sweepAndBatchNearbyOrders(assignedOrder, agentId);
                PathResult newRoute = routeOptimizationAgent.optimizeRouteForAgent(assignedOrder.getAssignedAgentId());

                System.out.println("Autonomous Pipeline Complete! Driver " + assignedOrder.getAssignedAgentId() +
                        " routed. | Total Cost: " + String.format("%.2f", newRoute.getTotalCost()));
            }
        } catch (Exception e) {
            System.out.println("ERROR in autonomous thread for " + newOrder.getId() + ": " + e.getMessage());
        }
    }

    // --- STUBS FOR STEP 4 (Failover & Locking) ---
    public boolean isOrderLocked(Order order) {
        // If the food is already physically in the car (or delivered), lock it down.
        return order.getStatus() == OrderStatus.IN_TRANSIT || order.getStatus() == OrderStatus.DELIVERED;
    }

    public void onAgentOffline(String agentId) {
        System.out.println("⚠️ EMERGENCY: Agent " + agentId + " went offline.");

        try {
            // 1. Fetch every order this driver is holding
            List<Order> abandonedOrders = orderRepository.findByAgentId(agentId);

            for (Order order : abandonedOrders) {
                // 2. Respect the physical reality of the food
                if (isOrderLocked(order)) {
                    System.out.println("🔒 Order " + order.getId() + " is physically in transit. Cannot reassign.");
                    continue;
                }

                System.out.println("♻️ Releasing abandoned order " + order.getId() + " back to the pool.");

                // 3. Strip the dead agent from the order and reset to PENDING
                order.setAssignedAgentId(null);
                order.setStatus(OrderStatus.PENDING);
                orderRepository.save(order);

                // 4. Immediately trigger our Brain to find a new, online driver!
                onNewOrder(order);
            }
        } catch (Exception e) {
            System.err.println("❌ Failover protocol crashed for agent " + agentId + ": " + e.getMessage());
        }
    }

    // 🌟 OPPORTUNISTIC BATCHING 🌟
    private void sweepAndBatchNearbyOrders(Order primaryOrder, String winningAgentId) {
        System.out.println("🧹 Sweeping for batchable orders at the same pickup location...");
        try {
            // Fetch all currently PENDING orders
            var future = db.collection("orders").whereEqualTo("status", "PENDING").get();
            var documents = future.get().getDocuments();

            for (var doc : documents) {
                Order pendingOrder = doc.toObject(Order.class);

                // Skip the order we literally just assigned
                if (pendingOrder.getId().equals(primaryOrder.getId())) continue;

                // Check if they are at the exact same pickup coordinates
                boolean isSameLat = pendingOrder.getPickupLocation().getLatitude() == primaryOrder.getPickupLocation().getLatitude();
                boolean isSameLon = pendingOrder.getPickupLocation().getLongitude() == primaryOrder.getPickupLocation().getLongitude();

                if (isSameLat && isSameLon) {
                    System.out.println("📦 BATCH SUCCESS: Bundling order " + pendingOrder.getId() + " with " + primaryOrder.getId() + " for Agent " + winningAgentId);

                    // Assign it to the same driver
                    pendingOrder.setAssignedAgentId(winningAgentId);
                    pendingOrder.setStatus(org.delivery.deliverybackend.model.OrderStatus.ASSIGNED);

                    // Save the batched order to Firestore
                    db.collection("orders").document(pendingOrder.getId()).set(pendingOrder);

                    // We let the Agent's currentLoad increment logic handle itself here or assume the AssignmentAgent caught it.
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Batching sweep failed: " + e.getMessage());
        }
    }
}
