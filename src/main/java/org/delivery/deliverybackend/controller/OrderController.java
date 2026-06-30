package org.delivery.deliverybackend.controller;

import org.delivery.deliverybackend.model.*;
import org.delivery.deliverybackend.repository.AgentRepository;
import org.delivery.deliverybackend.repository.OrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*") // Allows React to call this without CORS blocking
public class OrderController {
    private final OrderRepository orderRepository;
    private final AgentRepository agentRepository;

    public OrderController(OrderRepository orderRepository, AgentRepository agentRepository) {
        this.orderRepository = orderRepository;
        this.agentRepository = agentRepository;
    }

    @PostMapping("/addOrder")
    public ResponseEntity<Order> addOrder(@RequestBody Order incomingOrder) throws Exception {
        if (incomingOrder.getId() == null || incomingOrder.getId().isEmpty()) {
            incomingOrder.setId("ord-" + System.currentTimeMillis());
        }
        incomingOrder.setStatus(OrderStatus.PENDING);
        orderRepository.save(incomingOrder); // Saves directly to the cloud
        return ResponseEntity.ok(incomingOrder);
    }

    // 2. Get all orders (Real Database Call)
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() throws Exception {
        System.out.println("Fetching REAL orders from Firestore");

        List<Order> orders = orderRepository.findAll();

        return ResponseEntity.ok(orders);
    }

    // 3. Update order status
    @PatchMapping("/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> payload) throws Exception {

        String newStatusStr = payload.get("status");
        System.out.println("Updating Order " + id + " to " + newStatusStr + " in Firestore");

        // 1. Convert string to Enum
        OrderStatus status = OrderStatus.valueOf(newStatusStr.toUpperCase());

        // 2. Update the order in the database
        orderRepository.updateStatus(id, status);

        // 3. Fetch the fresh version of the order
        Order updatedOrder = orderRepository.findById(id);

        // 4. If the order just freed up capacity, release the agent
        if ((status == OrderStatus.DELIVERED || status == OrderStatus.CANCELLED)
                && updatedOrder.getAssignedAgentId() != null) {

            Agent agent = agentRepository.findById(updatedOrder.getAssignedAgentId());

            if (agent != null) {
                int newLoad = Math.max(0, agent.getCurrentLoad() - 1);
                agent.setCurrentLoad(newLoad);

                if (newLoad < agent.getMaxCapacity()) {
                    agent.setStatus(AgentStatus.AVAILABLE);
                }

                agentRepository.save(agent);
                System.out.println("Released agent " + agent.getId()
                        + " -> load=" + newLoad + ", status=" + agent.getStatus());
            }
        }

        return ResponseEntity.ok(updatedOrder);
    }

    @PatchMapping("/updateAgentStatus")
    public ResponseEntity<Agent> updateAgentStatus(@RequestBody Map<String, String> payload) throws Exception {
        String agentId = payload.get("agentId");
        String newStatusStr = payload.get("status");

        System.out.println("📡 API Request: Updating status for Agent " + agentId + " to " + newStatusStr);

        // 1. Fetch the agent from Firestore
        Agent agent = agentRepository.findById(agentId);
        if (agent == null) {
            System.out.println("❌ Agent not found: " + agentId);
            return ResponseEntity.notFound().build();
        }

        // 2. Safely convert String to Enum
        try {
            AgentStatus newStatus = AgentStatus.valueOf(newStatusStr.toUpperCase());
            agent.setStatus(newStatus);
        } catch (IllegalArgumentException e) {
            System.out.println("❌ CRITICAL: Invalid agent status sent: " + newStatusStr);
            return ResponseEntity.badRequest().build();
        }

        // 3. Save to database
        agentRepository.save(agent);

        return ResponseEntity.ok(agent);
    }


}
