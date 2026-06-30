package org.delivery.deliverybackend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.io.IOException;

    @Configuration
    public class FirebaseConfig {

        @PostConstruct
        public void initialize() {
            try {
                InputStream serviceAccount = new ClassPathResource("firebase-service-account.json").getInputStream();

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseApp.initializeApp(options);
                    System.out.println("Firebase initialized successfully !! ");
                }
            } catch (IOException e) {
                System.err.println("❌ ERROR: Could not initialize Firebase. Check if the file is in src/main/resources/");
                e.printStackTrace();
            }
        }

        @Bean
        public Firestore firestore() {
            // This takes the initialized Firebase app and registers the Firestore database as a Spring Bean
            return FirestoreClient.getFirestore();
        }
    }

