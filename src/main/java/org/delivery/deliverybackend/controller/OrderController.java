package org.delivery.deliverybackend.controller;

import org.delivery.deliverybackend.model.Location;
import org.delivery.deliverybackend.model.Order;
import org.delivery.deliverybackend.model.OrderStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*") // Allows React to call this without CORS blocking
public class OrderController {

    // 1. Add a new order
    @PostMapping("/addOrder")
    public ResponseEntity<Order> addOrder(@RequestBody Order incomingOrder) {
        System.out.println("Stub: Received new order request");

        incomingOrder.setStatus(OrderStatus.PENDING);

        if (incomingOrder.getId() == null) {
            incomingOrder.setId("ORD-" + System.currentTimeMillis());
        }

        return ResponseEntity.ok(incomingOrder);
    }

    // 2. Get all orders (Returning a hardcoded list of 2 items)
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        System.out.println("Stub: Fetching all orders");

        Order order1 = new Order(
                "ORD-1001",
                new Location(28.6139, 77.2090), // Connaught Place
                new Location(28.5355, 77.3910), // Noida
                OrderStatus.PENDING,
                System.currentTimeMillis() + 3600000, // 1 hour from now
                1,
                null
        );

        Order order2 = new Order(
                "ORD-1002",
                new Location(28.6514, 77.1907), // Karol Bagh
                new Location(28.4595, 77.0266), // Gurgaon
                OrderStatus.ASSIGNED,
                System.currentTimeMillis() + 7200000, // 2 hours from now
                2,
                "agent-007"
        );

        return ResponseEntity.ok(Arrays.asList(order1, order2));
    }

    // 3. Update order status
    @PatchMapping("/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> payload) {

        String newStatusStr = payload.get("status");
        System.out.println("Stub: Updating order " + id + " to status " + newStatusStr);

        // Create a fake updated order to send back
        Order updatedOrder = new Order(
                id,
                new Location(28.0, 77.0),
                new Location(28.1, 77.1),
                OrderStatus.valueOf(newStatusStr.toUpperCase()), // Convert string to Enum
                System.currentTimeMillis(),
                1,
                "agent-001"
        );

        return ResponseEntity.ok(updatedOrder);
    }
}
