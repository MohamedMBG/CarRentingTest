package com.example.carrentingtest.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;

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

public class FaceVerificationUtil {

    private static final String TAG = "FaceVerification";
    private static final float FACE_MATCH_THRESHOLD = 0.75f; // Increased threshold
    private static final int MIN_FACE_SIZE = 100; // Minimum face size in pixels

    public static boolean verifyFaces(Context context, Uri selfieUri, Uri licenseFaceUri) {
        // Load and preprocess images
        Bitmap selfieBitmap = loadAndPreprocessImage(context, selfieUri);
        Bitmap licenseBitmap = loadAndPreprocessImage(context, licenseFaceUri);

        if (selfieBitmap == null || licenseBitmap == null) {
            Log.e(TAG, "Image loading failed");
            return false;
        }

        // Configure face detector
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(0.15f) // Relative to image size
                .build();

        FaceDetector detector = FaceDetection.getClient(options);

        try {
            // Detect faces
            Face selfieFace = detectMainFace(detector, selfieBitmap);
            Face licenseFace = detectMainFace(detector, licenseBitmap);

            if (selfieFace == null || licenseFace == null) {
                Log.e(TAG, "Face detection failed - Selfie: " + (selfieFace != null) +
                        ", License: " + (licenseFace != null));
                return false;
            }

            // Improved face comparison
            float similarity = compareFacesAdvanced(selfieFace, licenseFace);
            Log.d(TAG, String.format("Face similarity score: %.2f", similarity));

            return similarity >= FACE_MATCH_THRESHOLD;
        } finally {
            detector.close();
        }
    }

    private static Bitmap loadAndPreprocessImage(Context context, Uri uri) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            // First decode with just bounds to check orientation
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);

            // Reset stream
            inputStream.close();
            InputStream newStream = context.getContentResolver().openInputStream(uri);

            // Handle orientation
            int orientation = getExifOrientation(context, uri);
            Bitmap bitmap = BitmapFactory.decodeStream(newStream);

            if (orientation != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(orientation);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0,
                        bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }

            // Scale down if too large to prevent OOM
            int maxDimension = Math.max(bitmap.getWidth(), bitmap.getHeight());
            if (maxDimension > 1024) {
                float scale = 1024f / maxDimension;
                bitmap = Bitmap.createScaledBitmap(bitmap,
                        (int)(bitmap.getWidth() * scale),
                        (int)(bitmap.getHeight() * scale), true);
            }

            return bitmap;
        } catch (IOException e) {
            Log.e(TAG, "Error loading image", e);
            return null;
        }
    }

    private static int getExifOrientation(Context context, Uri uri) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            ExifInterface exif = new ExifInterface(inputStream);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading EXIF data", e);
            return 0;
        }
    }

    private static Face detectMainFace(FaceDetector detector, Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        final CountDownLatch latch = new CountDownLatch(1);
        final Face[] result = {null};

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    if (!faces.isEmpty()) {
                        // Find the largest face (most likely the main subject)
                        Face largestFace = null;
                        float maxArea = 0;

                        for (Face face : faces) {
                            float area = face.getBoundingBox().width() * face.getBoundingBox().height();
                            if (area > maxArea) {
                                maxArea = area;
                                largestFace = face;
                            }
                        }

                        if (largestFace != null && maxArea > MIN_FACE_SIZE) {
                            result[0] = largestFace;
                        }
                    }
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Face detection error", e);
                    latch.countDown();
                });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e(TAG, "Detection interrupted", e);
        }

        return result[0];
    }

    private static float compareFacesAdvanced(Face face1, Face face2) {
        // Weighted comparison of multiple facial features
        float similarity = 0f;
        int featureCount = 0;

        // 1. Compare face bounding box proportions (30% weight)
        float widthRatio = (float) face1.getBoundingBox().width() / face2.getBoundingBox().width();
        float heightRatio = (float) face1.getBoundingBox().height() / face2.getBoundingBox().height();
        similarity += 0.3f * (1f - Math.abs(1f - widthRatio));
        similarity += 0.3f * (1f - Math.abs(1f - heightRatio));
        featureCount += 2;

        // 2. Compare facial landmarks (40% weight)
        if (face1.getLeftEyeOpenProbability() != null && face2.getLeftEyeOpenProbability() != null) {
            similarity += 0.2f * (1f - Math.abs(
                    face1.getLeftEyeOpenProbability() - face2.getLeftEyeOpenProbability()));
            featureCount++;
        }

        if (face1.getRightEyeOpenProbability() != null && face2.getRightEyeOpenProbability() != null) {
            similarity += 0.2f * (1f - Math.abs(
                    face1.getRightEyeOpenProbability() - face2.getRightEyeOpenProbability()));
            featureCount++;
        }

        // 3. Compare smiling probability (20% weight)
        if (face1.getSmilingProbability() != null && face2.getSmilingProbability() != null) {
            similarity += 0.2f * (1f - Math.abs(
                    face1.getSmilingProbability() - face2.getSmilingProbability()));
            featureCount++;
        }

        // 4. Compare head rotation (10% weight)
        similarity += 0.1f * (1f - Math.abs(
                face1.getHeadEulerAngleY() - face2.getHeadEulerAngleY()) / 180f);
        featureCount++;

        // Normalize the similarity score
        return featureCount > 0 ? similarity / featureCount : 0f;
    }
}