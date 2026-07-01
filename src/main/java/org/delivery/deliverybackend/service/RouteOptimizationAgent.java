package org.delivery.deliverybackend.service;

import org.delivery.deliverybackend.model.Agent;
import org.delivery.deliverybackend.model.Location;
import org.delivery.deliverybackend.model.Order;
import org.delivery.deliverybackend.model.PathResult;
import org.delivery.deliverybackend.repository.AgentRepository;
import org.delivery.deliverybackend.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RouteOptimizationAgent {

    private final RoutingService routingService;
    private final OrderRepository orderRepository;
    private final AgentRepository agentRepository;
    private Location currentlocation;

    public RouteOptimizationAgent(RoutingService routingService, OrderRepository orderRepository,AgentRepository agentRepository) {
        this.routingService = routingService;
        this.orderRepository = orderRepository;
        this.agentRepository = agentRepository;
    }

    public PathResult optimizeRouteForAgent(String agentId) throws Exception {
        System.out.println("🗺️ RouteOptimizationAgent: Calculating master route for Agent " + agentId);

        List<Order> activeOrders = orderRepository.findByAgentId(agentId);
        activeOrders.removeIf(order -> "DELIVERED".equals(order.getStatus()));

        if (activeOrders.isEmpty()) {
            return new PathResult(List.of(), 0.0);
        }

        Order priorityOrder = activeOrders.get(0);

        // 1. Identify all three coordinates (Driver -> Restaurant -> Customer)
        // Note: Replace agent.getLatitude() with your actual Agent location getter if it differs
        Agent agent = agentRepository.findById(agentId);
        String agentNode = routingService.findNearestNode(agent.getCurrentLocation().getLatitude(), agent.getCurrentLocation().getLongitude());

        String pickupNode = routingService.findNearestNode(
                priorityOrder.getPickupLocation().getLatitude(),
                priorityOrder.getPickupLocation().getLongitude()
        );
        String dropoffNode = routingService.findNearestNode(
                priorityOrder.getDeliveryLocation().getLatitude(),
                priorityOrder.getDeliveryLocation().getLongitude()
        );

        // 2. Calculate Leg 1 (Driver to Restaurant)
        PathResult leg1 = routingService.findFastestRoute(agentNode, pickupNode);

        // 3. Calculate Leg 2 (Restaurant to Customer)
        PathResult leg2 = routingService.findFastestRoute(pickupNode, dropoffNode);

        // 4. Stitch the paths together (removing the duplicate midpoint node)
        List<String> combinedPath = new ArrayList<>(leg1.getPathNames());
        if (leg2.getPathNames().size() > 1) {
            combinedPath.addAll(leg2.getPathNames().subList(1, leg2.getPathNames().size()));
        }

        // 5. Combine the costs
        double totalDistanceCost = leg1.getTotalCost() + leg2.getTotalCost();

        System.out.println("🔗 Route Stitched! Leg 1 Cost: " + leg1.getTotalCost() + " | Leg 2 Cost: " + leg2.getTotalCost());

        return new PathResult(combinedPath, totalDistanceCost);
    }
}
