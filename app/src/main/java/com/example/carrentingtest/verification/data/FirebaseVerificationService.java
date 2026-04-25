package com.example.carrentingtest.verification.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.carrentingtest.verification.VerificationStatus;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of {@link VerificationService} that stores compressed review
 * evidence as chunked Firestore documents for projects without Firebase Storage.
 */
public class FirebaseVerificationService implements VerificationService {

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private final OnDeviceFaceMatcher faceMatcher;
    private final Context appContext;
    private static final int MAX_EVIDENCE_EDGE_PX = 900;
    private static final int EVIDENCE_JPEG_QUALITY = 72;
    private static final int EVIDENCE_CHUNK_SIZE = 240_000;

    public FirebaseVerificationService(@NonNull Context context) {
        this(context.getApplicationContext(),
                FirebaseAuth.getInstance(),
                FirebaseFirestore.getInstance(),
                new OnDeviceFaceMatcher(context));
    }

    FirebaseVerificationService(@NonNull Context appContext,
                                 @NonNull FirebaseAuth auth,
                                 @NonNull FirebaseFirestore firestore,
                                 @NonNull OnDeviceFaceMatcher faceMatcher) {
        this.appContext = appContext;
        this.auth = auth;
        this.firestore = firestore;
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
        String selfieEvidence;
        String licenseEvidence;
        try {
            selfieEvidence = encodeEvidenceImage(selfie);
            licenseEvidence = encodeEvidenceImage(licenseFront);
        } catch (IOException e) {
            saveVerificationResult(
                    uid,
                    liveData,
                    new FaceMatchResult(
                            false,
                            matchResult.getScore(),
                            "Unable to prepare verification images for review.",
                            VerificationStatus.REJECTED,
                            matchResult.getLivenessAction(),
                            matchResult.isLivenessPassed()),
                    false,
                    "Unable to prepare verification images for review.",
                    null,
                    null);
            return;
        }

        saveVerificationResult(uid, liveData, matchResult, true, null, selfieEvidence, licenseEvidence);
    }

    private void saveVerificationResult(@NonNull String uid,
                                        @NonNull MutableLiveData<VerificationResult> liveData,
                                        @NonNull FaceMatchResult matchResult,
                                        boolean evidencePrepared,
                                        String evidenceError,
                                        String selfieEvidence,
                                        String licenseEvidence) {
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
                        uid,
                        liveData,
                        matchResult,
                        evidencePrepared,
                        evidenceError,
                        selfieEvidence,
                        licenseEvidence,
                        finalStatus,
                        requestRef,
                        userSnapshot))
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
                                                String selfieEvidence,
                                                String licenseEvidence,
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
        payload.put("evidenceStorage", "firestore_chunked_base64_jpeg");
        payload.put("evidencePrepared", evidencePrepared);
        if (evidencePrepared) {
            payload.put("selfieEvidenceKey", "selfie");
            payload.put("licenseFrontEvidenceKey", "license_front");
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
        if (evidencePrepared) {
            putEvidenceChunks(batch, requestRef, "selfie", selfieEvidence);
            putEvidenceChunks(batch, requestRef, "license_front", licenseEvidence);
        }

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

    private void putEvidenceChunks(@NonNull WriteBatch batch,
                                   @NonNull DocumentReference requestRef,
                                   @NonNull String key,
                                   @NonNull String base64) {
        int totalChunks = Math.max(1, (int) Math.ceil(base64.length() / (double) EVIDENCE_CHUNK_SIZE));
        for (int index = 0; index < totalChunks; index++) {
            int start = index * EVIDENCE_CHUNK_SIZE;
            int end = Math.min(base64.length(), start + EVIDENCE_CHUNK_SIZE);
            Map<String, Object> chunk = new HashMap<>();
            chunk.put("key", key);
            chunk.put("index", index);
            chunk.put("totalChunks", totalChunks);
            chunk.put("contentType", "image/jpeg");
            chunk.put("data", base64.substring(start, end));
            batch.set(requestRef
                    .collection("evidence")
                    .document(key + "_" + String.format(java.util.Locale.US, "%03d", index)), chunk);
        }
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

    private String encodeEvidenceImage(@NonNull Uri uri) throws IOException {
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
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP);
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

