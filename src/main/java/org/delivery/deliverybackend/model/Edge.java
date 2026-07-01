package org.delivery.deliverybackend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Edge {
    private String fromNodeId;
    private String toNodeId;
    private double distanceKm;
    private double trafficWeight = 1.0; // Multiplier: 1.0 is normal, 1.5 is heavy traffic
}
