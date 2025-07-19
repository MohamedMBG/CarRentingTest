package com.example.carrentingtest.utils;

import android.content.Context;
import android.graphics.Bitmap;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class FaceNetUtil {
    private static final int INPUT_SIZE = 160;
    private static final int EMBEDDING_SIZE = 128;
    private final Interpreter tfLite;

    private FaceNetUtil(Interpreter t) {
        tfLite = t;
    }

    public static FaceNetUtil create(Context ctx) {
        try {
            MappedByteBuffer model = loadModelFile(ctx, "facenet.tflite");
            Interpreter.Options opts = new Interpreter.Options();
            opts.setNumThreads(4);
            return new FaceNetUtil(new Interpreter(model, opts));
        } catch (IOException e) {
            throw new RuntimeException("Load model failed", e);
        }
    }

    private static MappedByteBuffer loadModelFile(Context ctx, String fn) throws IOException {
        FileInputStream fis = new FileInputStream(ctx.getAssets().openFd(fn).getFileDescriptor());
        FileChannel fc = fis.getChannel();
        long start = ctx.getAssets().openFd(fn).getStartOffset();
        long len = ctx.getAssets().openFd(fn).getDeclaredLength();
        return fc.map(FileChannel.MapMode.READ_ONLY, start, len);
    }

    public float[] getEmbedding(Bitmap bm) {
        return getEnhancedEmbedding(bm);
    }

    public float[] getEnhancedEmbedding(Bitmap bm) {
        if (bm == null) throw new IllegalArgumentException("Bitmap is null");
        Bitmap scaled = Bitmap.createScaledBitmap(bm, INPUT_SIZE, INPUT_SIZE, true);

        ByteBuffer input = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * 3 * 4)
                .order(ByteOrder.nativeOrder());
        int[] pix = new int[INPUT_SIZE * INPUT_SIZE];
        scaled.getPixels(pix, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE);

        for (int p : pix) {
            input.putFloat((((p >> 16) & 0xFF) - 127.5f) / 128f); // R
            input.putFloat((((p >> 8) & 0xFF) - 127.5f) / 128f);  // G
            input.putFloat(((p & 0xFF) - 127.5f) / 128f);         // B
        }


        float[][] out = new float[1][EMBEDDING_SIZE];
        tfLite.run(input, out);

        return normalizeEmbedding(out[0]);
    }

    private float[] normalizeEmbedding(float[] embedding) {
        double sum = 0;
        for (float v : embedding) {
            sum += v * v;
        }
        float norm = (float) Math.sqrt(sum);

        float[] normalized = new float[embedding.length];
        if (norm == 0) return normalized; // prevent division by zero
        for (int i = 0; i < embedding.length; i++) {
            normalized[i] = embedding[i] / norm;
        }
        return normalized;
    }

    public static float cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) throw new IllegalArgumentException("Invalid vectors");
        float dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0; // prevent division by zero
        return (float) (dot / (Math.sqrt(normA) * Math.sqrt(normB)));
    }

    // Kept for backward compatibility
    public static float calcDistance(float[] a, float[] b) {
        return 1 - cosineSimilarity(a, b);
    }
}