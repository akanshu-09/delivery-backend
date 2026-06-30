package org.delivery.deliverybackend.controller;

import org.delivery.deliverybackend.model.Location;
import org.delivery.deliverybackend.model.Order;
import org.delivery.deliverybackend.model.OrderStatus;
import org.delivery.deliverybackend.repository.OrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*") // Allows React to call this without CORS blocking
public class OrderController {
    private final OrderRepository orderRepository;

    public OrderController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @PostMapping("/addOrder")
    public ResponseEntity<Order> addOrder(@RequestBody Order incomingOrder) throws Exception {
        if (incomingOrder.getId() == null || incomingOrder.getId().isEmpty()) {
            incomingOrder.setId("ord-" + System.currentTimeMillis());
        }
        incomingOrder.setStatus(OrderStatus.PENDING);
        orderRepository.save(incomingOrder); // Saves directly to the cloud
        return ResponseEntity.ok(incomingOrder);
    }

    // 2. Get all orders (Real Database Call)
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() throws Exception {
        System.out.println("Fetching REAL orders from Firestore");

        List<Order> orders = orderRepository.findAll();

        return ResponseEntity.ok(orders);
    }

    // 3. Update order status
    @PatchMapping("/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> payload) throws Exception {

        String newStatusStr = payload.get("status");
        System.out.println("Updating Order " + id + " to " + newStatusStr + " in Firestore");

        // 1. Convert string to Enum
        OrderStatus status = OrderStatus.valueOf(newStatusStr.toUpperCase());

        // 2. Update the database
        orderRepository.updateStatus(id, status);

        // 3. Fetch the fresh version to return to the frontend
        Order updatedOrder = orderRepository.findById(id);

        return ResponseEntity.ok(updatedOrder);
    }
}
