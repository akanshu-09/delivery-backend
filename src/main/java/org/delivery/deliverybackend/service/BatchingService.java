package org.delivery.deliverybackend.service;

import org.delivery.deliverybackend.model.Order;
import org.delivery.deliverybackend.model.OrderStatus;
import org.delivery.deliverybackend.util.HaversineUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BatchingService {

    // Maximum distance (in kilometers) between order pickups to allow them in the same batch
    private static final double BATCH_DISTANCE_THRESHOLD_KM = 2.0;

    public List<List<Order>> clusterOrders(List<Order> pendingOrders) {
        System.out.println("📦 RUNNING GREEDY BATCHER: Processing " + pendingOrders.size() + " pending orders...");

        List<List<Order>> batches = new ArrayList<>();
        boolean[] visited = new boolean[pendingOrders.size()];

        for (int i = 0; i < pendingOrders.size(); i++) {
            if (visited[i]) continue;

            // Start a brand-new batch with this unassigned order
            List<Order> currentBatch = new ArrayList<>();
            Order baseOrder = pendingOrders.get(i);
            currentBatch.add(baseOrder);
            visited[i] = true;

            System.out.println("Starting new cluster anchored by Order: " + baseOrder.getId());

            // Look through all remaining orders to see who is close enough to join this batch
            for (int j = i + 1; j < pendingOrders.size(); j++) {
                if (visited[j]) continue;

                Order comparisonOrder = pendingOrders.get(j);

                // Calculate distance between the two pickup coordinates
                double distanceBetweenPickups = HaversineUtil.calculateDistance(
                        baseOrder.getPickupLocation(),
                        comparisonOrder.getPickupLocation()
                );

                if (distanceBetweenPickups <= BATCH_DISTANCE_THRESHOLD_KM) {
                    currentBatch.add(comparisonOrder);
                    visited[j] = true;
                    System.out.println("  -> Order " + comparisonOrder.getId() + " added to batch. Distance: " + String.format("%.2f", distanceBetweenPickups) + " km");
                }
            }

            batches.add(currentBatch);
        }

        System.out.println("BATCHING COMPLETE: Created " + batches.size() + " optimal fulfillment clusters.");
        return batches;
    }
}
