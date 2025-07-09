package com.example.carrentingtest.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.CountDownLatch;

/** Utility for face verification using TFLite face embeddings */
public class FaceVerificationUtil {
    private static Interpreter tflite = null;
    private static final String MODEL_FILE = "facenet.tflite"; // Make sure this file is in your assets

    // Loads the model if not already loaded
    private static Interpreter getTFLiteInterpreter(Context context) throws IOException {
        if (tflite == null) {
            MappedByteBuffer buffer = loadModelFile(context, MODEL_FILE);
            tflite = new Interpreter(buffer);
        }
        return tflite;
    }
    // Loads model file from assets
    private static MappedByteBuffer loadModelFile(Context context, String modelFile) throws IOException {
        FileInputStream inputStream = context.getAssets().openFd(modelFile).createInputStream();
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = context.getAssets().openFd(modelFile).getStartOffset();
        long declaredLength = context.getAssets().openFd(modelFile).getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
    // Detect face in bitmap and return the cropped face bitmap (blocking)
    private static Bitmap detectAndCropFace(Bitmap bitmap, Context context) throws Exception {
        final Bitmap[] resultBitmap = {null};
        CountDownLatch latch = new CountDownLatch(1);

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .build();
        FaceDetector detector = FaceDetection.getClient(options);

        InputImage image = InputImage.fromBitmap(bitmap, 0);
        detector.process(image)
                .addOnSuccessListener(faces -> {
                    if (faces.size() > 0) {
                        Face face = faces.get(0);
                        int x = Math.max(face.getBoundingBox().left, 0);
                        int y = Math.max(face.getBoundingBox().top, 0);
                        int w = Math.min(face.getBoundingBox().width(), bitmap.getWidth() - x);
                        int h = Math.min(face.getBoundingBox().height(), bitmap.getHeight() - y);
                        resultBitmap[0] = Bitmap.createBitmap(bitmap, x, y, w, h);
                    }
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        latch.await(); // Block until done
        if (resultBitmap[0] == null) throw new Exception("No face detected!");
        return resultBitmap[0];
    }
    // Generate embedding for a face image
    private static float[] getFaceEmbedding(Bitmap faceBitmap, Context context) throws IOException {
        Bitmap resized = Bitmap.createScaledBitmap(faceBitmap, 112, 112, true); // Most facenet models use 112x112
        float[][][][] input = new float[1][112][112][3];
        // Preprocess image (normalize pixel values)
        for (int y = 0; y < 112; y++) {
            for (int x = 0; x < 112; x++) {
                int pixel = resized.getPixel(x, y);
                input[0][y][x][0] = ((pixel >> 16) & 0xFF) / 255.0f;
                input[0][y][x][1] = ((pixel >> 8) & 0xFF) / 255.0f;
                input[0][y][x][2] = (pixel & 0xFF) / 255.0f;
            }
        }

        float[][] embedding = new float[1][128]; // 128 for facenet, 512 for some others
        getTFLiteInterpreter(context).run(input, embedding);
        return embedding[0];
    }
    // Compute cosine similarity between two vectors
    private static float cosineSimilarity(float[] vec1, float[] vec2) {
        float dot = 0f, normA = 0f, normB = 0f;
        for (int i = 0; i < vec1.length; i++) {
            dot += vec1[i] * vec2[i];
            normA += vec1[i] * vec1[i];
            normB += vec2[i] * vec2[i];
        }
        return (float)(dot / (Math.sqrt(normA) * Math.sqrt(normB)));
    }
    // Main public method: returns true if faces match, false otherwise
    public static boolean isFaceMatch(Bitmap selfie, Bitmap license, Context context) {
        try {
            Bitmap faceSelfie = detectAndCropFace(selfie, context);
            Bitmap faceLicense = detectAndCropFace(license, context);

            float[] embeddingSelfie = getFaceEmbedding(faceSelfie, context);
            float[] embeddingLicense = getFaceEmbedding(faceLicense, context);

            float similarity = cosineSimilarity(embeddingSelfie, embeddingLicense);
            Log.d("FaceVerification", "Similarity score: " + similarity); // Debug log

            float threshold = 0.4f; // Lowered from 0.6 to 0.4 (less strict)
            return similarity > threshold;
        } catch (Exception e) {
            Log.e("FaceVerification", "Error in face matching", e);
            return false; // Fail gracefully
        }
    }
    // (Optional) Add a version that returns the score
    public static float getFaceSimilarityScore(Bitmap selfie, Bitmap license, Context context) {
        try {
            Bitmap faceSelfie = detectAndCropFace(selfie, context);
            Bitmap faceLicense = detectAndCropFace(license, context);

            float[] embeddingSelfie = getFaceEmbedding(faceSelfie, context);
            float[] embeddingLicense = getFaceEmbedding(faceLicense, context);

            return cosineSimilarity(embeddingSelfie, embeddingLicense);

        } catch (Exception e) {
            e.printStackTrace();
            return 0f;
        }
    }
}