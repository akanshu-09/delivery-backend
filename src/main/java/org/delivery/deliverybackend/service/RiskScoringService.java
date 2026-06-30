package org.delivery.deliverybackend.service;

import org.delivery.deliverybackend.model.Order;
import org.springframework.stereotype.Service;

@Service
public class RiskScoringService { //gtihub

    // Assuming average urban delivery speed of 30 km/h (which is 0.5 km per minute)
    private static final double AVERAGE_SPEED_KM_PER_MIN = 0.5;

    public String calculateRiskLevel(Order order, double routeDistanceKm) {

        long currentTimeMillis = System.currentTimeMillis();

        // 1. Calculate how long the drive will actually take
        double estimatedTravelTimeMins = routeDistanceKm / AVERAGE_SPEED_KM_PER_MIN;

        // 2. Add that drive time to the current clock to get the exact ETA
        long estimatedArrivalTime = currentTimeMillis + (long) (estimatedTravelTimeMins * 60 * 1000);

        // 3. Compare ETA to the customer's strict deadline
        long timeRemainingBuffer = order.getDeadlineTimestamp() - estimatedArrivalTime;
        long minutesBuffer = timeRemainingBuffer / (60 * 1000);

        System.out.print("⏱️ RISK ENGINE -> Order " + order.getId() + " | Buffer: " + minutesBuffer + " mins | Status: ");

        // 4. Return the calculated threat level
        if (minutesBuffer < 0) {
            System.out.println("🚨 CRITICAL (Late)");
            return "CRITICAL";
        } else if (minutesBuffer <= 15) {
            System.out.println("🟠 HIGH RISK");
            return "HIGH_RISK";
        } else if (minutesBuffer <= 45) {
            System.out.println("🟡 MEDIUM RISK");
            return "MEDIUM_RISK";
        } else {
            System.out.println("🟢 LOW RISK (Safe)");
            return "LOW_RISK";
        }
    }
}