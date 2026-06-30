package org.delivery.deliverybackend.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.delivery.deliverybackend.model.Agent;
import org.delivery.deliverybackend.model.AgentStatus;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Repository
public class AgentRepository {

    private static final String COLLECTION_NAME = "agents";

    private Firestore getDb() {
        return FirestoreClient.getFirestore();
    }


    public String save(Agent agent) throws ExecutionException, InterruptedException {
        Firestore db = getDb();
        ApiFuture<WriteResult> future = db.collection(COLLECTION_NAME).document(agent.getId()).set(agent);
        return future.get().getUpdateTime().toString();
    }


    public Agent findById(String id) throws ExecutionException, InterruptedException {
        Firestore db = getDb();
        DocumentReference docRef = db.collection(COLLECTION_NAME).document(id);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();

        if (document.exists()) {
            return document.toObject(Agent.class);
        }
        return null;
    }


    public List<Agent> findAvailableAgents() throws ExecutionException, InterruptedException {
        Firestore db = getDb();
        // Here we query Firestore directly so we don't download offline agents into memory
        ApiFuture<QuerySnapshot> future = db.collection(COLLECTION_NAME)
                .whereEqualTo("status", AgentStatus.AVAILABLE.name())
                .get();

        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        List<Agent> availableAgents = new ArrayList<>();

        for (QueryDocumentSnapshot document : documents) {
            availableAgents.add(document.toObject(Agent.class));
        }
        return availableAgents;
    }


    public void updateLoad(String id, int newLoad) {
        Firestore db = getDb();
        db.collection(COLLECTION_NAME).document(id).update("currentLoad", newLoad);
    }

    // 5. Get all agents in the database (For the React Map)
    public List<Agent> findAll() throws ExecutionException, InterruptedException {
        Firestore db = getDb();
        ApiFuture<QuerySnapshot> future = db.collection(COLLECTION_NAME).get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        List<Agent> agents = new ArrayList<>();
        for (QueryDocumentSnapshot document : documents) {
            agents.add(document.toObject(Agent.class));
        }
        return agents;
    }
}
