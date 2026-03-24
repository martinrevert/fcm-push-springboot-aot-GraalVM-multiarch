package com.example.movienotifier.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirebaseConfig {
    @Value("${firebase.service-account-file}")
    private String serviceAccountFile;

    /**
     * Initializes the default Firebase application from the configured service account file.
     *
     * @throws IOException when the credentials file cannot be read
     */
    @PostConstruct
    public void init() throws IOException {
        FileInputStream serviceAccount = new FileInputStream(serviceAccountFile);
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }
    }

    /**
     * Exposes the FirebaseMessaging singleton as a Spring bean.
     *
     * @return FirebaseMessaging instance bound to the default Firebase app
     */
    @Bean
    public FirebaseMessaging firebaseMessaging() {
        return FirebaseMessaging.getInstance();
    }
}

