package org.delivery.deliverybackend.controller;

import org.delivery.deliverybackend.model.Agent;
import org.delivery.deliverybackend.model.AgentStatus;
import org.delivery.deliverybackend.model.Location;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/agents")
@CrossOrigin(origins = "*") // CORS
public class AgentController {

    @GetMapping
    public ResponseEntity<List<Agent>> getAllAgents() {
        System.out.println("Stub: Fetching all active agents");

        Agent agent1 = new Agent(
                "agent-001",
                new Location(28.6139, 77.2090), // Connaught Place
                AgentStatus.AVAILABLE,
                0,
                5
        );

        Agent agent2 = new Agent(
                "agent-007",
                new Location(28.6514, 77.1907), // Karol Bagh
                AgentStatus.BUSY,
                3,
                5
        );

        return ResponseEntity.ok(Arrays.asList(agent1, agent2));
    }


    @PutMapping("/updateAgent")
    public ResponseEntity<Agent> updateAgent(@RequestBody Agent incomingAgent) {
        System.out.println("Stub: Received location/status update for agent: " + incomingAgent.getId());
        return ResponseEntity.ok(incomingAgent);
    }
}
