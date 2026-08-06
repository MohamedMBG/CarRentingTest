package com.bbluxurycars.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Initializes the Firebase Admin SDK so the backend can verify ID tokens
 * issued to the Android app and manage custom claims (role, companyId).
 *
 * Credentials are optional at boot time (not via GOOGLE_APPLICATION_CREDENTIALS
 * auto-detection alone) so the service can still start locally without a
 * service account configured; anything that needs Firebase will fail per-request
 * instead of blocking startup.
 */
@Component
public class FirebaseAdminConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseAdminConfig.class);

    @Value("${firebase.credentials-path:}")
    private String credentialsPath;

    @PostConstruct
    public void init() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }
        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.warn("firebase.credentials-path not set; Firebase Admin SDK not initialized. "
                    + "Token verification endpoints will fail until configured.");
            return;
        }
        try (FileInputStream serviceAccount = new FileInputStream(credentialsPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            FirebaseApp.initializeApp(options);
            log.info("Firebase Admin SDK initialized from {}", credentialsPath);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize Firebase Admin SDK from "
                    + credentialsPath, e);
        }
    }
}
