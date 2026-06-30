package org.delivery.deliverybackend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order
{
    private String id;
    private Location pickupLocation;
    private Location deliveryLocation;
    private OrderStatus status;
    private long deadlineTimestamp;
    private Integer priority;
    private String assignedAgentId;
}
