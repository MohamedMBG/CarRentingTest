package com.bbluxurycars.backend.firestore;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.GeoPoint;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.FirestoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * {@link FirestoreGateway} over the Firebase Admin SDK.
 *
 * <p>Reads block on the SDK's futures. That is appropriate here: the mirror
 * runs inside a request or an admin-triggered sync that has nothing else to do
 * until the data arrives, and an asynchronous API would only move the wait.
 *
 * <p>A failed read returns empty rather than propagating. Firestore being
 * momentarily unreachable must not turn into a 500 on {@code /v1/me}: the
 * caller simply stays unprovisioned and keeps using the app's existing
 * Firestore path, which is exactly the pre-mirror behaviour.
 */
@Component
public class FirebaseFirestoreGateway implements FirestoreGateway {

    private static final Logger log = LoggerFactory.getLogger(FirebaseFirestoreGateway.class);

    @Override
    public boolean isAvailable() {
        return !FirebaseApp.getApps().isEmpty();
    }

    @Override
    public Optional<FirestoreDocument> findDocument(String collection, String id) {
        if (!isAvailable() || id == null || id.isBlank()) {
            return Optional.empty();
        }
        try {
            DocumentSnapshot snapshot = firestore().collection(collection).document(id).get().get();
            if (!snapshot.exists()) {
                return Optional.empty();
            }
            return Optional.of(new FirestoreDocument(snapshot.getId(), unwrap(snapshot.getData())));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted reading {}/{} from Firestore", collection, id);
            return Optional.empty();
        } catch (ExecutionException | RuntimeException e) {
            log.warn("Could not read {}/{} from Firestore: {}", collection, id, e.toString());
            return Optional.empty();
        }
    }

    @Override
    public List<FirestoreDocument> findWhereEquals(String collection, String field, String value) {
        if (!isAvailable() || value == null || value.isBlank()) {
            return List.of();
        }
        try {
            List<QueryDocumentSnapshot> documents = firestore().collection(collection)
                    .whereEqualTo(field, value)
                    .get()
                    .get()
                    .getDocuments();
            return documents.stream()
                    .map(snapshot -> new FirestoreDocument(snapshot.getId(), unwrap(snapshot.getData())))
                    .toList();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted querying {} where {}={} from Firestore", collection, field, value);
            return List.of();
        } catch (ExecutionException | RuntimeException e) {
            log.warn("Could not query {} where {}={}: {}", collection, field, value, e.toString());
            return List.of();
        }
    }

    private static Firestore firestore() {
        return FirestoreClient.getFirestore();
    }

    /**
     * Replaces Firestore's own value types with plain ones, so that nothing
     * downstream needs the SDK on its classpath to read a mirrored field.
     * Only {@code GeoPoint} needs translating today; everything else the
     * mirrored collections hold is already a String, Number or Boolean.
     */
    private static Map<String, Object> unwrap(Map<String, Object> data) {
        if (data == null) {
            return Map.of();
        }
        Map<String, Object> unwrapped = new LinkedHashMap<>(new HashMap<>(data));
        unwrapped.replaceAll((key, value) -> value instanceof GeoPoint point
                ? new FirestoreDocument.GeoPointValue(point.getLatitude(), point.getLongitude())
                : value);
        return unwrapped;
    }
}
