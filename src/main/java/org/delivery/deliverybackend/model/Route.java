package org.delivery.deliverybackend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Route
{
    private String id;
    private String agentId;
    private List<String> orderIds;
    private List<Location> waypoints;
    private int estimatedDuration;
    private RouteStatus status;
    private long createdAt;
    private double totalDistanceKm;
    private long estimatedArrivalTimestamp;
    private String optimizationMode;
}
