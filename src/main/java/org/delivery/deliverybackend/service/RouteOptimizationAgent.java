package org.delivery.deliverybackend.service;

import org.delivery.deliverybackend.model.Order;
import org.delivery.deliverybackend.model.PathResult;
import org.delivery.deliverybackend.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RouteOptimizationAgent {

    private final RoutingService routingService;
    private final OrderRepository orderRepository;

    public RouteOptimizationAgent(RoutingService routingService, OrderRepository orderRepository) {
        this.routingService = routingService;
        this.orderRepository = orderRepository;
    }

    public PathResult optimizeRouteForAgent(String agentId) throws Exception {
        System.out.println("🗺️ RouteOptimizationAgent: Calculating master route for Agent " + agentId);

        // 1. Fetch all orders for this agent
        List<Order> activeOrders = orderRepository.findByAgentId(agentId);

        // 2. Filter out DELIVERED orders (we only care about pending/in-transit)
        activeOrders.removeIf(order -> "DELIVERED".equals(order.getStatus()));

        if (activeOrders.isEmpty()) {
            System.out.println("RouteOptimizationAgent: No active orders for agent.");
            return new PathResult(List.of(), 0.0);
        }

        // 3. For Day 6 simplicity: Route from the Agent's current node to the FIRST order's dropoff.
        // (In a full production app, this would use a TSP (Traveling Salesperson) solver for multiple dropoffs)
        Order priorityOrder = activeOrders.get(0);

        // Find nearest nodes (using the bridge method we built yesterday)
        String pickupNode = routingService.findNearestNode(
                priorityOrder.getPickupLocation().getLatitude(),
                priorityOrder.getPickupLocation().getLongitude()
        );
        String dropoffNode = routingService.findNearestNode(
                priorityOrder.getDeliveryLocation().getLatitude(),
                priorityOrder.getDeliveryLocation().getLongitude()
        );

        // 4. Run the A* Math
        return routingService.findFastestRoute(pickupNode, dropoffNode);
    }
}
