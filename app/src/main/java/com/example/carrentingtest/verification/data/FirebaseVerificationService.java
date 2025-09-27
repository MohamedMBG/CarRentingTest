package com.example.carrentingtest.verification.data;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.carrentingtest.storage.StoragePaths;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of {@link VerificationService} that relies on Firebase products
 * (Storage + Firestore) to submit verification requests and observe their status.
 */
public class FirebaseVerificationService implements VerificationService {

    private final FirebaseAuth auth;
    private final FirebaseStorage storage;
    private final FirebaseFirestore firestore;

    public FirebaseVerificationService() {
        this(FirebaseAuth.getInstance(), FirebaseStorage.getInstance(), FirebaseFirestore.getInstance());
    }

    FirebaseVerificationService(@NonNull FirebaseAuth auth,
                                 @NonNull FirebaseStorage storage,
                                 @NonNull FirebaseFirestore firestore) {
        this.auth = auth;
        this.storage = storage;
        this.firestore = firestore;
    }

    @Override
    public LiveData<VerificationResult> submit(@NonNull Uri selfie, @NonNull Uri licenseFront) {
        MutableLiveData<VerificationResult> liveData = new MutableLiveData<>();
        String uid = auth.getUid();
        if (uid == null) {
            liveData.setValue(new VerificationResult(VerificationResult.Status.FAILED));
            return liveData;
        }

        StorageReference selfieRef = storage.getReference().child(StoragePaths.selfiePath(uid));
        StorageReference licenseRef = storage.getReference().child(StoragePaths.licenseFrontPath(uid));

        UploadTask selfieTask = selfieRef.putFile(selfie);
        UploadTask licenseTask = licenseRef.putFile(licenseFront);

        Tasks.whenAllComplete(selfieTask, licenseTask)
                .addOnSuccessListener(tasks -> handleUploadCompletion(uid, liveData, selfieTask, licenseTask));

        return liveData;
    }

    private void handleUploadCompletion(@NonNull String uid,
                                        @NonNull MutableLiveData<VerificationResult> liveData,
                                        @NonNull UploadTask selfieTask,
                                        @NonNull UploadTask licenseTask) {
        if (!selfieTask.isSuccessful() || !licenseTask.isSuccessful()) {
            liveData.setValue(new VerificationResult(VerificationResult.Status.FAILED));
            return;
        }

        DocumentReference requestRef = firestore.collection("verification_requests").document(uid);
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", uid);
        payload.put("status", "PENDING");
        payload.put("selfiePath", StoragePaths.selfiePath(uid));
        payload.put("licenseFrontPath", StoragePaths.licenseFrontPath(uid));
        payload.put("submittedAt", FieldValue.serverTimestamp());
        payload.put("updatedAt", FieldValue.serverTimestamp());

        requestRef.set(payload, SetOptions.merge())
                .addOnSuccessListener(unused -> attachListener(requestRef, liveData))
                .addOnFailureListener(e -> liveData.setValue(new VerificationResult(VerificationResult.Status.FAILED)));
    }

    private void attachListener(@NonNull DocumentReference requestRef,
                                @NonNull MutableLiveData<VerificationResult> liveData) {
        liveData.postValue(new VerificationResult(VerificationResult.Status.PENDING));

        AtomicReference<ListenerRegistration> registrationRef = new AtomicReference<>();
        ListenerRegistration registration = requestRef.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                liveData.postValue(new VerificationResult(VerificationResult.Status.FAILED));
                ListenerRegistration current = registrationRef.getAndSet(null);
                if (current != null) current.remove();
                return;
            }

            if (snapshot == null || !snapshot.exists()) {
                return;
            }

            VerificationResult.Status status = mapStatus(snapshot.getString("status"));
            liveData.postValue(new VerificationResult(status));

            if (status == VerificationResult.Status.VERIFIED || status == VerificationResult.Status.FAILED) {
                ListenerRegistration current = registrationRef.getAndSet(null);
                if (current != null) current.remove();
            }
        });

        registrationRef.set(registration);
    }

    private VerificationResult.Status mapStatus(String statusValue) {
        if (statusValue == null) {
            return VerificationResult.Status.PENDING;
        }

        if ("VERIFIED".equalsIgnoreCase(statusValue) || "APPROVED".equalsIgnoreCase(statusValue)) {
            return VerificationResult.Status.VERIFIED;
        }
        if ("FAILED".equalsIgnoreCase(statusValue) || "REJECTED".equalsIgnoreCase(statusValue)) {
            return VerificationResult.Status.FAILED;
        }
        return VerificationResult.Status.PENDING;
    }
}

