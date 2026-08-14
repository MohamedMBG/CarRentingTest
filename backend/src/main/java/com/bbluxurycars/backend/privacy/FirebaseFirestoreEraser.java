package com.bbluxurycars.backend.privacy;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.cloud.StorageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * {@link FirestoreEraser} over the Firebase Admin SDK.
 *
 * <p>Every failure is caught and logged rather than propagated, same as
 * {@code FirebaseFirestoreGateway}: erasure should get as far as it can
 * rather than leaving a caller with nothing removed because one collection
 * was briefly unreachable. {@link com.bbluxurycars.backend.privacy.PrivacyService}
 * is what decides, in aggregate, whether that is good enough to report success.
 */
@Component
public class FirebaseFirestoreEraser implements FirestoreEraser {

    private static final Logger log = LoggerFactory.getLogger(FirebaseFirestoreEraser.class);

    @Override
    public boolean isAvailable() {
        return !FirebaseApp.getApps().isEmpty();
    }

    @Override
    public void deleteDocument(String collection, String id) {
        if (!isAvailable()) {
            return;
        }
        try {
            firestore().collection(collection).document(id).delete().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted deleting {}/{}", collection, id);
        } catch (ExecutionException | RuntimeException e) {
            log.warn("Could not delete {}/{}: {}", collection, id, e.toString());
        }
    }

    @Override
    public void deleteCollection(String collectionPath) {
        if (!isAvailable()) {
            return;
        }
        try {
            List<QueryDocumentSnapshot> documents = firestore().collection(collectionPath)
                    .get().get().getDocuments();
            for (QueryDocumentSnapshot document : documents) {
                document.getReference().delete().get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted deleting collection {}", collectionPath);
        } catch (ExecutionException | RuntimeException e) {
            log.warn("Could not delete collection {}: {}", collectionPath, e.toString());
        }
    }

    @Override
    public void deleteStoragePrefix(String prefix) {
        if (!isAvailable()) {
            return;
        }
        try {
            Bucket bucket = StorageClient.getInstance().bucket();
            for (Blob blob : bucket.list(Storage.BlobListOption.prefix(prefix)).iterateAll()) {
                blob.delete();
            }
        } catch (RuntimeException e) {
            log.warn("Could not delete storage objects under {}: {}", prefix, e.toString());
        }
    }

    @Override
    public void deleteAuthUser(String uid) {
        if (!isAvailable()) {
            return;
        }
        try {
            FirebaseAuth.getInstance().deleteUser(uid);
        } catch (FirebaseAuthException e) {
            log.warn("Could not delete Firebase Auth user {}: {}", uid, e.toString());
        }
    }

    private static Firestore firestore() {
        return FirestoreClient.getFirestore();
    }
}
