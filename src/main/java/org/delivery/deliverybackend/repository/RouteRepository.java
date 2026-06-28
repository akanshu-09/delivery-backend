package org.delivery.deliverybackend.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.delivery.deliverybackend.model.Route;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Repository
public class RouteRepository {

    private static final String COLLECTION_NAME = "routes";

    private Firestore getDb() {
        return FirestoreClient.getFirestore();
    }


    public String save(Route route) throws ExecutionException, InterruptedException {
        Firestore db = getDb();
        ApiFuture<WriteResult> future = db.collection(COLLECTION_NAME).document(route.getId()).set(route);
        return future.get().getUpdateTime().toString();
    }

    public Route findByAgentId(String agentId) throws ExecutionException, InterruptedException {
        Firestore db = getDb();
        ApiFuture<QuerySnapshot> future = db.collection(COLLECTION_NAME)
                .whereEqualTo("agentId", agentId)
                .whereEqualTo("status", "ACTIVE")
                .limit(1) // An agent should only have 1 active route at a time
                .get();

        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        if (!documents.isEmpty()) {
            return documents.get(0).toObject(Route.class);
        }
        return null;
    }
}
