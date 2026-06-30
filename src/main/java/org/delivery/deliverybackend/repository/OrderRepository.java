package org.delivery.deliverybackend.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.delivery.deliverybackend.model.Order;
import org.delivery.deliverybackend.model.OrderStatus;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Repository
public class OrderRepository {

    private static final String COLLECTION_NAME = "orders";

    private Firestore getDb() {
        return FirestoreClient.getFirestore();
    }

    // 1. Save a new order to Firestore
    public String save(Order order) throws ExecutionException, InterruptedException {
        Firestore db = getDb();
        ApiFuture<WriteResult> future = db.collection(COLLECTION_NAME).document(order.getId()).set(order);
        return future.get().getUpdateTime().toString();
    }


    public Order findById(String id) throws ExecutionException, InterruptedException {
        Firestore db = getDb();
        DocumentReference docRef = db.collection(COLLECTION_NAME).document(id);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();

        if (document.exists()) {
            return document.toObject(Order.class);
        }
        return null;
    }

    public List<Order> findAll() throws ExecutionException, InterruptedException {
        Firestore db = getDb();
        ApiFuture<QuerySnapshot> future = db.collection(COLLECTION_NAME).get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        List<Order> orders = new ArrayList<>();
        for (QueryDocumentSnapshot document : documents) {
            orders.add(document.toObject(Order.class));
        }
        return orders;
    }

    public void updateStatus(String id, OrderStatus status) {
        Firestore db = getDb();
        db.collection(COLLECTION_NAME).document(id).update("status", status.name());
    }

    public void updateAssignedAgent(String id, String agentId) {
        Firestore db = getDb();
        db.collection(COLLECTION_NAME).document(id).update("assignedAgentId", agentId);
    }

    // Fetch all orders assigned to a specific driver
    public List<Order> findByAgentId(String agentId) throws Exception {
        System.out.println("🔍 Querying Firestore for orders assigned to: " + agentId);
        Firestore db = getDb();
        List<Order> agentOrders = new ArrayList<>();
        ApiFuture<QuerySnapshot> future = db.collection("orders")
                .whereEqualTo("assignedAgentId", agentId)
                .get();

        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        for (QueryDocumentSnapshot document : documents) {
            agentOrders.add(document.toObject(Order.class));
        }
        return agentOrders;
    }
}
