package org.delivery.deliverybackend.service;

import org.delivery.deliverybackend.model.Agent;
import org.delivery.deliverybackend.model.AgentStatus;
import org.delivery.deliverybackend.model.Order;
import org.delivery.deliverybackend.model.OrderStatus;
import org.delivery.deliverybackend.repository.AgentRepository;
import org.delivery.deliverybackend.repository.OrderRepository;
import org.delivery.deliverybackend.util.HaversineUtil;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class AssignmentAgent {

    private final OrderRepository orderRepository;
    private final AgentRepository agentRepository;

    public AssignmentAgent(OrderRepository orderRepository, AgentRepository agentRepository) {
        this.orderRepository = orderRepository;
        this.agentRepository = agentRepository;
    }

    public Order assignBestAgent(String orderId) throws ExecutionException, InterruptedException {
        // Get the exact order
        Order order = orderRepository.findById(orderId);
        if (order == null || order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Order not found or not PENDING");
        }

        // Get all currently AVAILABLE agents
        List<Agent> availableAgents = agentRepository.findAvailableAgents();
        if (availableAgents.isEmpty()) {
            failOrder(order);
            return order;
        }

        // Run the Scoring Engine
        // 🛡️ PRE-LOOP DEFENSE: Does the order itself have valid coordinates?
        if (order.getPickupLocation() == null || order.getDeliveryLocation() == null) {
            System.out.println("❌ CRITICAL: Order " + order.getId() + " has corrupted GPS data. Cannot assign.");
            failOrder(order);
            return order;
        }

        double highestScore = -1.0;
        Agent bestAgent = null;

        for (Agent agent : availableAgents) {

            // 🛡️ IN-LOOP DEFENSE: Skip this specific agent if their GPS is offline/corrupted
            if (agent.getCurrentLocation() == null) {
                System.out.println("⚠️ WARNING: Skipping Agent " + agent.getId() + " due to missing GPS coordinates.");
                continue;
            }

            // We are now 100% safe from NullPointerExceptions. Run the math!
            double distanceKm = HaversineUtil.calculateDistance(agent.getCurrentLocation(), order.getPickupLocation());

            // (Assume average city speed is 40 km/h)
            double distanceToDropoff = HaversineUtil.calculateDistance(order.getPickupLocation(), order.getDeliveryLocation());
            double totalTravelKm = distanceKm + distanceToDropoff;
            long estimatedTravelTimeMillis = (long) ((totalTravelKm / 40.0) * 3600000); // Convert hours to ms

            double deadlineScore = 1.0;
            if (System.currentTimeMillis() + estimatedTravelTimeMillis > order.getDeadlineTimestamp()) {
                deadlineScore = 0.0;
            }

            // If this agent cannot make the delivery in time, skip them entirely
            if (deadlineScore == 0.0) {
                continue;
            }

            double distanceScore = 1.0 / (distanceKm + 1.0); // +1 prevents divide-by-zero
            double workloadScore = (double) (agent.getMaxCapacity() - agent.getCurrentLoad()) / agent.getMaxCapacity();

            // Exact weight formula: 0.4(Dist) + 0.3(Workload) + 0.3(Deadline)
            double totalScore = (0.4 * distanceScore) + (0.3 * workloadScore) + (0.3 * deadlineScore);

            System.out.println("Agent: " + agent.getId() + " | Score: " + totalScore);

            if (totalScore > highestScore) {
                highestScore = totalScore;
                bestAgent = agent;
            }
        }

        if (bestAgent != null) {
            System.out.println(" WINNER: " + bestAgent.getId() + " assigned to " + order.getId());

            order.setAssignedAgentId(bestAgent.getId());
            order.setStatus(OrderStatus.ASSIGNED);
            orderRepository.save(order); // Save to Firestore

            bestAgent.setCurrentLoad(bestAgent.getCurrentLoad() + 1);

            if (bestAgent.getCurrentLoad() >= bestAgent.getMaxCapacity()) {
                bestAgent.setStatus(AgentStatus.BUSY);
            }
            agentRepository.save(bestAgent);

            return order;
        } else {
            System.out.println("FAILED: No agents available or capable of meeting the deadline.");
            failOrder(order);
            return order;
        }
    }

    private void failOrder(Order order) throws ExecutionException, InterruptedException {
        System.out.println("FAILED: No suitable agents found for order " + order.getId());
        order.setStatus(OrderStatus.FAILED);
        orderRepository.save(order);
    }
}
