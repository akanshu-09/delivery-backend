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
    private String name;
    private Location currentLocation;
    private AgentStatus status;
    private Integer currentLoad;
    private Integer maxCapacity;
}
