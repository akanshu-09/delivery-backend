package org.delivery.deliverybackend.controller;

import org.delivery.deliverybackend.model.Agent;
import org.delivery.deliverybackend.repository.AgentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agents")
@CrossOrigin(origins = "*")
public class AgentController {

    // 1. Inject the Database
    private final AgentRepository agentRepository;

    public AgentController(AgentRepository agentRepository)
    {
        this.agentRepository = agentRepository;
    }

    // 2. Get all agents (Real Database Call)
    @GetMapping
    public ResponseEntity<List<Agent>> getAllAgents() throws Exception {
        System.out.println("Fetching REAL active agents from Firestore");

        List<Agent> agents = agentRepository.findAll();

        return ResponseEntity.ok(agents);
    }

    // 3. Update agent state (Real Database Save)
    @PutMapping("/updateAgent")
    public ResponseEntity<Agent> updateAgent(@RequestBody Agent incomingAgent) throws Exception {
        System.out.println("Saving REAL location/status update for agent: " + incomingAgent.getId());

        agentRepository.save(incomingAgent);

        return ResponseEntity.ok(incomingAgent);
    }
}
