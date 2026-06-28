package org.delivery.deliverybackend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Agent
{
    private String id;
    private Location currentLocation;
    private AgentStatus status;
    private int currentLoad;
    private int maxCapacity;
}
