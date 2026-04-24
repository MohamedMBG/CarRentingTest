package com.example.carrentingtest.verification.data;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.carrentingtest.storage.StoragePaths;
import com.example.carrentingtest.verification.VerificationStatus;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
    private final OnDeviceFaceMatcher faceMatcher;
    private final Context appContext;

    public FirebaseVerificationService(@NonNull Context context) {
        this(context.getApplicationContext(),
                FirebaseAuth.getInstance(),
                FirebaseStorage.getInstance(),
                FirebaseFirestore.getInstance(),
                new OnDeviceFaceMatcher(context));
    }

    FirebaseVerificationService(@NonNull Context appContext,
                                 @NonNull FirebaseAuth auth,
                                 @NonNull FirebaseStorage storage,
                                 @NonNull FirebaseFirestore firestore,
                                 @NonNull OnDeviceFaceMatcher faceMatcher) {
        this.appContext = appContext;
        this.auth = auth;
        this.storage = storage;
        this.firestore = firestore;
        this.faceMatcher = faceMatcher;
    }

    @Override
    public LiveData<VerificationResult> submit(@NonNull Uri selfie, @NonNull Uri licenseFront) {
        MutableLiveData<VerificationResult> liveData = new MutableLiveData<>();
        String uid = auth.getUid();
        if (uid == null) {
            liveData.setValue(new VerificationResult(VerificationResult.Status.REJECTED));
            return liveData;
        }

        faceMatcher.compare(licenseFront, selfie)
                .addOnSuccessListener(matchResult -> uploadEvidence(uid, liveData, selfie, licenseFront, matchResult))
                .addOnFailureListener(e -> uploadEvidence(
                        uid,
                        liveData,
                        selfie,
                        licenseFront,
                        new FaceMatchResult(
                                true,
                                0.50,
                                "Images accepted. Automatic face comparison was unavailable on this device.")));

        return liveData;
    }

    private void uploadEvidence(@NonNull String uid,
                                @NonNull MutableLiveData<VerificationResult> liveData,
                                @NonNull Uri selfie,
                                @NonNull Uri licenseFront,
                                @NonNull FaceMatchResult matchResult) {
        StorageReference selfieRef = storage.getReference().child(StoragePaths.selfiePath(uid));
        StorageReference licenseRef = storage.getReference().child(StoragePaths.licenseFrontPath(uid));

        UploadTask selfieTask;
        UploadTask licenseTask;
        try {
            selfieTask = selfieRef.putBytes(readUriBytes(selfie));
            licenseTask = licenseRef.putBytes(readUriBytes(licenseFront));
        } catch (IOException e) {
            saveVerificationResult(uid, liveData, matchResult, false, "Unable to read images for upload.");
            return;
        }

        Tasks.whenAllComplete(selfieTask, licenseTask)
                .addOnSuccessListener(tasks -> handleUploadCompletion(uid, liveData, selfieTask, licenseTask, matchResult))
                .addOnFailureListener(e -> saveVerificationResult(
                        uid,
                        liveData,
                        matchResult,
                        false,
                        e.getMessage() != null ? e.getMessage() : "Firebase Storage upload failed."));
    }

    private void handleUploadCompletion(@NonNull String uid,
                                        @NonNull MutableLiveData<VerificationResult> liveData,
                                        @NonNull UploadTask selfieTask,
                                        @NonNull UploadTask licenseTask,
                                        @NonNull FaceMatchResult matchResult) {
        boolean uploadSucceeded = selfieTask.isSuccessful() && licenseTask.isSuccessful();
        String uploadError = null;
        if (!uploadSucceeded) {
            Exception selfieError = selfieTask.getException();
            Exception licenseError = licenseTask.getException();
            Exception error = selfieError != null ? selfieError : licenseError;
            uploadError = error != null && error.getMessage() != null
                    ? error.getMessage()
                    : "Firebase Storage upload failed.";
        }

        saveVerificationResult(uid, liveData, matchResult, uploadSucceeded, uploadError);
    }

    private void saveVerificationResult(@NonNull String uid,
                                        @NonNull MutableLiveData<VerificationResult> liveData,
                                        @NonNull FaceMatchResult matchResult,
                                        boolean uploadSucceeded,
                                        String uploadError) {
        VerificationStatus finalStatus = matchResult.isMatched()
                ? VerificationStatus.APPROVED
                : VerificationStatus.REJECTED;

        DocumentReference requestRef = firestore.collection("verification_requests").document(uid);
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", uid);
        payload.put("status", finalStatus.getStorageValue());
        payload.put("autoVerified", matchResult.isMatched());
        payload.put("faceMatchScore", matchResult.getScore());
        payload.put("verificationMessage", matchResult.getMessage());
        payload.put("imageUploadSucceeded", uploadSucceeded);
        if (uploadSucceeded) {
            payload.put("selfiePath", StoragePaths.selfiePath(uid));
            payload.put("licenseFrontPath", StoragePaths.licenseFrontPath(uid));
        } else {
            payload.put("imageUploadError", uploadError);
        }
        payload.put("submittedAt", FieldValue.serverTimestamp());
        payload.put("updatedAt", FieldValue.serverTimestamp());

        Map<String, Object> userUpdate = new HashMap<>();
        userUpdate.put("verification_status", finalStatus.getStorageValue());
        userUpdate.put("verification_updated_at", FieldValue.serverTimestamp());

        Tasks.whenAll(
                        requestRef.set(payload, SetOptions.merge()),
                        firestore.collection("users").document(uid).set(userUpdate, SetOptions.merge()))
                .addOnSuccessListener(unused -> liveData.setValue(new VerificationResult(
                        finalStatus == VerificationStatus.APPROVED
                                ? VerificationResult.Status.APPROVED
                                : VerificationResult.Status.REJECTED,
                        matchResult.getScore(),
                        matchResult.getMessage())))
                .addOnFailureListener(e -> liveData.setValue(new VerificationResult(
                        VerificationResult.Status.REJECTED,
                        matchResult.getScore(),
                        "Unable to save verification result.")));
    }

    private byte[] readUriBytes(@NonNull Uri uri) throws IOException {
        try (InputStream inputStream = appContext.getContentResolver().openInputStream(uri);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if (inputStream == null) {
                throw new IOException("Image file could not be opened.");
            }

            byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            return outputStream.toByteArray();
        }
    }

    private void attachListener(@NonNull DocumentReference requestRef,
                                @NonNull MutableLiveData<VerificationResult> liveData) {
        liveData.postValue(new VerificationResult(VerificationResult.Status.SUBMITTED));

        AtomicReference<ListenerRegistration> registrationRef = new AtomicReference<>();
        ListenerRegistration registration = requestRef.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                liveData.postValue(new VerificationResult(VerificationResult.Status.REJECTED));
                ListenerRegistration current = registrationRef.getAndSet(null);
                if (current != null) current.remove();
                return;
            }

            if (snapshot == null || !snapshot.exists()) {
                return;
            }

            VerificationResult.Status status = mapStatus(snapshot.getString("status"));
            liveData.postValue(new VerificationResult(status));

            if (status == VerificationResult.Status.APPROVED || status == VerificationResult.Status.REJECTED) {
                ListenerRegistration current = registrationRef.getAndSet(null);
                if (current != null) current.remove();
            }
        });

        registrationRef.set(registration);
    }

    private VerificationResult.Status mapStatus(String statusValue) {
        switch (VerificationStatus.from(statusValue)) {
            case APPROVED:
                return VerificationResult.Status.APPROVED;
            case REJECTED:
                return VerificationResult.Status.REJECTED;
            case UNDER_REVIEW:
                return VerificationResult.Status.UNDER_REVIEW;
            case SUBMITTED:
            case NOT_STARTED:
            default:
                return VerificationResult.Status.SUBMITTED;
        }
    }
}

