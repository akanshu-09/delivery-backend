package org.delivery.deliverybackend.controller;

import org.delivery.deliverybackend.model.Agent;
import org.delivery.deliverybackend.model.AgentStatus;
import org.delivery.deliverybackend.model.Order;
import org.delivery.deliverybackend.model.OrderStatus;
import org.delivery.deliverybackend.repository.AgentRepository;
import org.delivery.deliverybackend.repository.OrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/agents")
@CrossOrigin(origins = "*")
public class AgentController {

    // 1. Inject the Database
    private final AgentRepository agentRepository;
    private final OrderRepository orderRepository;

    public AgentController(AgentRepository agentRepository,OrderRepository orderRepository)
    {
        this.agentRepository = agentRepository;
        this.orderRepository = orderRepository;
    }

    // 2. Get all agents (Real Database Call)
    @GetMapping
    public ResponseEntity<List<Agent>> getAllAgents() throws Exception {
        System.out.println("Fetching REAL active agents from Firestore");

        List<Agent> agents = agentRepository.findAll();

        return ResponseEntity.ok(agents);
    }

    // 4. Add a NEW Agent dynamically
    @PostMapping("/addAgent")
    public ResponseEntity<Agent> addAgent(@RequestBody Agent incomingAgent) throws Exception {
        incomingAgent.setCurrentLoad(0);

        String providedName = incomingAgent.getName();
        if (providedName != null && !providedName.trim().isEmpty()) {
            String slug = slugify(providedName);
            String candidateId = "agent-" + slug;
            int suffix = 2;
            while (agentRepository.findById(candidateId) != null) {
                candidateId = "agent-" + slug + "-" + suffix;
                suffix++;
            }
            incomingAgent.setId(candidateId);
            incomingAgent.setName(providedName.trim());
        } else if (incomingAgent.getId() == null || incomingAgent.getId().isEmpty()) {
            String nextId = generateNextAgentId();
            incomingAgent.setId(nextId);
            incomingAgent.setName(nextId);
        }

        System.out.println("Adding NEW agent to Firestore: " + incomingAgent.getId());
        agentRepository.save(incomingAgent);
        return ResponseEntity.ok(incomingAgent);
    }

    private String slugify(String input) {
        return input.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
    }

    private String generateNextAgentId() throws Exception {
        List<Agent> allAgents = agentRepository.findAll();
        int maxNumber = 0;
        for (Agent a : allAgents) {
            String id = a.getId();
            if (id != null && id.startsWith("agent-")) {
                try {
                    int n = Integer.parseInt(id.substring("agent-".length()));
                    maxNumber = Math.max(maxNumber, n);
                } catch (NumberFormatException ignored) {
                    // name-based id like "agent-rahul" — not a number, skip it
                }
            }
        }
        return String.format("agent-%03d", maxNumber + 1);
    }

    // 3. Update agent state (Real Database Save)
    @PutMapping("/updateAgent")
    public ResponseEntity<Agent> updateAgent(@RequestBody Agent incomingAgent) throws Exception {
        System.out.println("Saving REAL location/status update for agent: " + incomingAgent.getId());

        agentRepository.save(incomingAgent);

        return ResponseEntity.ok(incomingAgent);
    }

    @PostMapping("/agents/recalculateAllLoads")
    public ResponseEntity<List<Agent>> recalculateAllLoads() throws Exception {
        List<Agent> allAgents = agentRepository.findAll();
        List<Agent> updated = new ArrayList<>();

        for (Agent agent : allAgents) {
            List<Order> agentOrders = orderRepository.findByAgentId(agent.getId());
            long activeCount = agentOrders.stream()
                    .filter(o -> o.getStatus() != OrderStatus.DELIVERED
                            && o.getStatus() != OrderStatus.CANCELLED
                            && o.getStatus() != OrderStatus.FAILED)
                    .count();

            agent.setCurrentLoad((int) activeCount);
            agent.setStatus(activeCount >= agent.getMaxCapacity() ? AgentStatus.BUSY : AgentStatus.AVAILABLE);
            agentRepository.save(agent);
            updated.add(agent);
        }

        return ResponseEntity.ok(updated);
    }
}
