package ar.com.martinrevert.movienotifier.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

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
        try (InputStream serviceAccount = resolveServiceAccountStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
        }
    }

    /**
     * Resolves Firebase credentials first from the configured filesystem path, then from classpath.
     *
     * @return credential stream for Firebase Admin SDK initialization
     * @throws IOException when no readable credentials source is found
     */
    private InputStream resolveServiceAccountStream() throws IOException {
        Path configuredPath = Path.of(serviceAccountFile);
        if (Files.exists(configuredPath) && Files.isReadable(configuredPath)) {
            return Files.newInputStream(configuredPath);
        }

        InputStream classpathStream = getClass().getClassLoader().getResourceAsStream(serviceAccountFile);
        if (classpathStream != null) {
            return classpathStream;
        }

        throw new IOException("Firebase service account file not found. Checked path '"
                + serviceAccountFile
                + "'. Set FIREBASE_SERVICE_ACCOUNT_FILE or GOOGLE_APPLICATION_CREDENTIALS to a mounted readable file.");
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


