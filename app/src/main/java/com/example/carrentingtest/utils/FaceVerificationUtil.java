package com.example.carrentingtest.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class FaceVerificationUtil {

    private static final String TAG = "FaceVerificationUtil";
    private static final float FACE_MATCH_THRESHOLD = 0.7f; // Adjust this threshold as needed

    /**
     * Compares faces from selfie and license images
     *
     * @param context        Application context
     * @param selfieUri      URI of the selfie image
     * @param licenseFaceUri URI of the license image
     * @return true if faces match with sufficient confidence
     */
    public static boolean verifyFaces(Context context, Uri selfieUri, Uri licenseFaceUri) {
        // Load bitmaps from URIs
        Bitmap selfieBitmap = getBitmapFromUri(context, selfieUri);
        Bitmap licenseBitmap = getBitmapFromUri(context, licenseFaceUri);

        if (selfieBitmap == null || licenseBitmap == null) {
            Log.e(TAG, "Failed to load one or both images");
            return false;
        }

        // Configure face detector
        FaceDetectorOptions highAccuracyOpts = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();

        FaceDetector detector = FaceDetection.getClient(highAccuracyOpts);

        try {
            // Detect faces in both images
            Face selfieFace = detectSingleFace(detector, selfieBitmap);
            Face licenseFace = detectSingleFace(detector, licenseBitmap);

            if (selfieFace == null || licenseFace == null) {
                Log.e(TAG, "Could not detect faces in one or both images");
                return false;
            }

            // Compare facial landmarks (simplified comparison)
            float similarity = compareFaces(selfieFace, licenseFace);
            Log.d(TAG, "Face similarity score: " + similarity);

            return similarity >= FACE_MATCH_THRESHOLD;
        } finally {
            detector.close();
        }
    }

    private static Bitmap getBitmapFromUri(Context context, Uri uri) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            return BitmapFactory.decodeStream(inputStream);
        } catch (IOException e) {
            Log.e(TAG, "Error loading image from URI", e);
            return null;
        }
    }

    private static Face detectSingleFace(FaceDetector detector, Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        final CountDownLatch latch = new CountDownLatch(1);
        final Face[] result = {null};

        Task<List<Face>> task = detector.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                    @Override
                    public void onSuccess(List<Face> faces) {
                        if (!faces.isEmpty()) {
                            result[0] = faces.get(0); // Get the first face
                        }
                        latch.countDown();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Face detection failed", e);
                        latch.countDown();
                    }
                });

        try {
            latch.await(); // Wait for the detection to complete
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e(TAG, "Face detection interrupted", e);
        }

        return result[0];
    }

    private static float compareFaces(Face face1, Face face2) {
        // Simplified comparison using facial landmarks
        // In a real app, you would use a more sophisticated algorithm

        float distance = 0f;
        int landmarkCount = 0;

        // Compare positions of similar landmarks
        if (face1.getLeftEyeOpenProbability() != null && face2.getLeftEyeOpenProbability() != null) {
            distance += Math.abs(face1.getLeftEyeOpenProbability() - face2.getLeftEyeOpenProbability());
            landmarkCount++;
        }

        if (face1.getRightEyeOpenProbability() != null && face2.getRightEyeOpenProbability() != null) {
            distance += Math.abs(face1.getRightEyeOpenProbability() - face2.getRightEyeOpenProbability());
            landmarkCount++;
        }

        if (face1.getSmilingProbability() != null && face2.getSmilingProbability() != null) {
            distance += Math.abs(face1.getSmilingProbability() - face2.getSmilingProbability());
            landmarkCount++;
        }

        // Normalize the distance (0 = perfect match, 1 = completely different)
        float normalizedDistance = landmarkCount > 0 ? distance / landmarkCount : 1f;

        // Convert distance to similarity (1 - distance)
        return 1f - normalizedDistance;
    }
}