package com.example.carrentingtest.verification.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.carrentingtest.verification.VerificationStatus;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class FirebaseVerificationService implements VerificationService {

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private final FirebaseStorage storage;
    private final OnDeviceFaceMatcher faceMatcher;
    private final Context appContext;
    private static final int MAX_EVIDENCE_EDGE_PX = 900;
    private static final int EVIDENCE_JPEG_QUALITY = 72;

    public FirebaseVerificationService(@NonNull Context context) {
        this(context.getApplicationContext(),
                FirebaseAuth.getInstance(),
                FirebaseFirestore.getInstance(),
                FirebaseStorage.getInstance(),
                new OnDeviceFaceMatcher(context));
    }

    FirebaseVerificationService(@NonNull Context appContext,
                                 @NonNull FirebaseAuth auth,
                                 @NonNull FirebaseFirestore firestore,
                                 @NonNull FirebaseStorage storage,
                                 @NonNull OnDeviceFaceMatcher faceMatcher) {
        this.appContext = appContext;
        this.auth = auth;
        this.firestore = firestore;
        this.storage = storage;
        this.faceMatcher = faceMatcher;
    }

    @Override
    public LiveData<VerificationResult> submit(@NonNull Uri selfie,
                                               @NonNull Uri licenseFront,
                                               @NonNull LivenessAction livenessAction) {
        MutableLiveData<VerificationResult> liveData = new MutableLiveData<>();
        String uid = auth.getUid();
        if (uid == null) {
            liveData.setValue(new VerificationResult(VerificationResult.Status.REJECTED));
            return liveData;
        }

        faceMatcher.compare(licenseFront, selfie, livenessAction)
                .addOnSuccessListener(matchResult -> uploadEvidence(uid, liveData, selfie, licenseFront, matchResult))
                .addOnFailureListener(e -> uploadEvidence(
                        uid,
                        liveData,
                        selfie,
                        licenseFront,
                        new FaceMatchResult(
                                false,
                                0.0,
                                "Automatic face comparison was unavailable. Your documents need manual review.",
                                VerificationStatus.UNDER_REVIEW,
                                livenessAction,
                                false)));

        return liveData;
    }

    private void uploadEvidence(@NonNull String uid,
                                @NonNull MutableLiveData<VerificationResult> liveData,
                                @NonNull Uri selfie,
                                @NonNull Uri licenseFront,
                                @NonNull FaceMatchResult matchResult) {
        byte[] selfieBytes;
        byte[] licenseBytes;
        try {
            selfieBytes = compressImage(selfie);
            licenseBytes = compressImage(licenseFront);
        } catch (IOException e) {
            saveVerificationResult(
                    uid, liveData,
                    new FaceMatchResult(
                            false,
                            matchResult.getScore(),
                            "Unable to prepare verification images for review.",
                            VerificationStatus.REJECTED,
                            matchResult.getLivenessAction(),
                            matchResult.isLivenessPassed()),
                    false, "Unable to prepare verification images for review.", null, null);
            return;
        }

        StorageReference selfieRef = storage.getReference()
                .child("verification_evidence").child(uid).child("selfie.jpg");
        StorageReference licenseRef = storage.getReference()
                .child("verification_evidence").child(uid).child("license_front.jpg");

        Task<Uri> selfieUrlTask = selfieRef.putBytes(selfieBytes)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    return selfieRef.getDownloadUrl();
                });

        Task<Uri> licenseUrlTask = licenseRef.putBytes(licenseBytes)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    return licenseRef.getDownloadUrl();
                });

        Tasks.whenAll(selfieUrlTask, licenseUrlTask)
                .addOnSuccessListener(unused -> {
                    String selfieUrl = selfieUrlTask.getResult().toString();
                    String licenseUrl = licenseUrlTask.getResult().toString();
                    saveVerificationResult(uid, liveData, matchResult, true, null, selfieUrl, licenseUrl);
                })
                .addOnFailureListener(e -> saveVerificationResult(
                        uid, liveData,
                        new FaceMatchResult(
                                false,
                                matchResult.getScore(),
                                "Unable to upload verification images.",
                                VerificationStatus.REJECTED,
                                matchResult.getLivenessAction(),
                                matchResult.isLivenessPassed()),
                        false, "Unable to upload verification images.", null, null));
    }

    private void saveVerificationResult(@NonNull String uid,
                                        @NonNull MutableLiveData<VerificationResult> liveData,
                                        @NonNull FaceMatchResult matchResult,
                                        boolean evidencePrepared,
                                        String evidenceError,
                                        String selfieUrl,
                                        String licenseUrl) {
        VerificationStatus resolvedStatus = matchResult.getRecommendedStatus();
        if (resolvedStatus == VerificationStatus.APPROVED) {
            resolvedStatus = VerificationStatus.UNDER_REVIEW;
        }
        if (resolvedStatus == VerificationStatus.UNDER_REVIEW && !evidencePrepared) {
            resolvedStatus = VerificationStatus.REJECTED;
        }
        final VerificationStatus finalStatus = resolvedStatus;

        DocumentReference requestRef = firestore.collection("verification_requests").document(uid);
        firestore.collection("users").document(uid)
                .get()
                .addOnSuccessListener(userSnapshot -> saveVerificationResultWithUser(
                        uid, liveData, matchResult, evidencePrepared, evidenceError,
                        selfieUrl, licenseUrl, finalStatus, requestRef, userSnapshot))
                .addOnFailureListener(e -> liveData.setValue(new VerificationResult(
                        VerificationResult.Status.REJECTED,
                        matchResult.getScore(),
                        "Unable to load user profile for verification.")));
    }

    private void saveVerificationResultWithUser(@NonNull String uid,
                                                @NonNull MutableLiveData<VerificationResult> liveData,
                                                @NonNull FaceMatchResult matchResult,
                                                boolean evidencePrepared,
                                                String evidenceError,
                                                String selfieUrl,
                                                String licenseUrl,
                                                @NonNull VerificationStatus finalStatus,
                                                @NonNull DocumentReference requestRef,
                                                @NonNull DocumentSnapshot userSnapshot) {
        String companyId = userSnapshot.getString("companyId");
        if (companyId == null || companyId.trim().isEmpty()) {
            liveData.setValue(new VerificationResult(
                    VerificationResult.Status.REJECTED,
                    matchResult.getScore(),
                    "No company is linked to this account."));
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", uid);
        payload.put("companyId", companyId);
        payload.put("userName", userSnapshot.getString("name"));
        payload.put("userEmail", userSnapshot.getString("email"));
        payload.put("userPhone", userSnapshot.getString("phone"));
        payload.put("driverLicense", userSnapshot.getString("driverLicense"));
        payload.put("status", finalStatus.getStorageValue());
        payload.put("autoCheckPassed", matchResult.getRecommendedStatus() == VerificationStatus.APPROVED && matchResult.isMatched());
        payload.put("autoVerified", false);
        payload.put("manualReviewRequired", finalStatus == VerificationStatus.UNDER_REVIEW);
        payload.put("livenessAction", matchResult.getLivenessAction() != null
                ? matchResult.getLivenessAction().getStorageValue()
                : null);
        payload.put("livenessPassed", matchResult.isLivenessPassed());
        payload.put("faceMatchScore", matchResult.getScore());
        payload.put("verificationMessage", matchResult.getMessage());
        payload.put("evidenceStorage", "firebase_storage_jpeg");
        payload.put("evidencePrepared", evidencePrepared);
        if (evidencePrepared) {
            payload.put("selfieUrl", selfieUrl);
            payload.put("licenseFrontUrl", licenseUrl);
        } else {
            payload.put("evidenceError", evidenceError);
        }
        payload.put("submittedAt", FieldValue.serverTimestamp());
        payload.put("updatedAt", FieldValue.serverTimestamp());

        Map<String, Object> userUpdate = new HashMap<>();
        userUpdate.put("verification_status", finalStatus.getStorageValue());
        userUpdate.put("verification_updated_at", FieldValue.serverTimestamp());

        WriteBatch batch = firestore.batch();
        batch.set(requestRef, payload, SetOptions.merge());
        batch.set(firestore.collection("users").document(uid), userUpdate, SetOptions.merge());

        batch.commit()
                .addOnSuccessListener(unused -> liveData.setValue(new VerificationResult(
                        toResultStatus(finalStatus),
                        matchResult.getScore(),
                        matchResult.getMessage())))
                .addOnFailureListener(e -> liveData.setValue(new VerificationResult(
                        VerificationResult.Status.REJECTED,
                        matchResult.getScore(),
                        "Unable to save verification result.")));
    }

    private VerificationResult.Status toResultStatus(@NonNull VerificationStatus status) {
        switch (status) {
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

    private byte[] compressImage(@NonNull Uri uri) throws IOException {
        Bitmap original;
        try (InputStream inputStream = appContext.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                throw new IOException("Image file could not be opened.");
            }
            original = BitmapFactory.decodeStream(inputStream);
        }
        if (original == null) {
            throw new IOException("Image file could not be decoded.");
        }

        Bitmap scaled = scaleDown(original, MAX_EVIDENCE_EDGE_PX);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.JPEG, EVIDENCE_JPEG_QUALITY, outputStream);
        if (scaled != original) {
            scaled.recycle();
        }
        original.recycle();
        return outputStream.toByteArray();
    }

    private Bitmap scaleDown(@NonNull Bitmap source, int maxEdgePx) {
        int width = source.getWidth();
        int height = source.getHeight();
        int longestEdge = Math.max(width, height);
        if (longestEdge <= maxEdgePx) {
            return source;
        }

        float scale = maxEdgePx / (float) longestEdge;
        int targetWidth = Math.max(1, Math.round(width * scale));
        int targetHeight = Math.max(1, Math.round(height * scale));
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true);
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
