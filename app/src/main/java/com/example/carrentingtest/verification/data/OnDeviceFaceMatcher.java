package com.example.carrentingtest.verification.data;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.example.carrentingtest.verification.VerificationStatus;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.io.IOException;
import java.util.List;

/**
 * Lightweight on-device face gate for the mobile verification flow.
 *
 * ML Kit face detection does not provide biometric embeddings. This class keeps
 * the flow practical by requiring a readable face on the license and one fresh
 * selfie face, then using landmark geometry when ML Kit can read enough points.
 */
public class OnDeviceFaceMatcher {
    private static final double MATCH_THRESHOLD = 0.88;
    private static final float MIN_HEAD_TURN_DEGREES = 10f;
    private static final float MIN_SMILE_PROBABILITY = 0.65f;

    private final Context appContext;
    private final FaceDetector detector;

    public OnDeviceFaceMatcher(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(0.02f)
                .build();
        this.detector = FaceDetection.getClient(options);
    }

    public Task<FaceMatchResult> compare(@NonNull Uri licenseUri,
                                         @NonNull Uri selfieUri,
                                         @NonNull LivenessAction livenessAction) {
        Task<List<Face>> licenseTask;
        Task<List<Face>> selfieTask;
        try {
            licenseTask = detectFaces(licenseUri);
            selfieTask = detectFaces(selfieUri);
        } catch (IOException e) {
            return Tasks.forResult(new FaceMatchResult(
                    false,
                    0.0,
                    "Automatic face comparison could not read one image. Your documents need manual review.",
                    VerificationStatus.UNDER_REVIEW,
                    livenessAction,
                    false));
        }

        return Tasks.whenAllSuccess(licenseTask, selfieTask)
                .continueWith(task -> {
                    if (!task.isSuccessful() || task.getResult() == null || task.getResult().size() < 2) {
                        return new FaceMatchResult(
                                false,
                                0.0,
                                "Automatic face comparison was unavailable. Your documents need manual review.",
                                VerificationStatus.UNDER_REVIEW,
                                livenessAction,
                                false);
                    }

                    @SuppressWarnings("unchecked")
                    List<Face> licenseFaces = (List<Face>) task.getResult().get(0);
                    @SuppressWarnings("unchecked")
                    List<Face> selfieFaces = (List<Face>) task.getResult().get(1);
                    return compareFaces(licenseFaces, selfieFaces, livenessAction);
                });
    }

    private Task<List<Face>> detectFaces(@NonNull Uri uri) throws IOException {
        InputImage image = InputImage.fromFilePath(appContext, uri);
        return detector.process(image);
    }

    private FaceMatchResult compareFaces(@NonNull List<Face> licenseFaces,
                                         @NonNull List<Face> selfieFaces,
                                         @NonNull LivenessAction livenessAction) {
        if (licenseFaces.isEmpty()) {
            if (selfieFaces.isEmpty()) {
                return rejected("No face detected in the selfie.", livenessAction, false);
            }
            if (selfieFaces.size() > 1) {
                return rejected("Only one person should appear in the selfie.", livenessAction, false);
            }
            return new FaceMatchResult(
                    false,
                    0.0,
                    "The driver license portrait could not be detected. Your documents need manual review.",
                    VerificationStatus.UNDER_REVIEW,
                    livenessAction,
                    false);
        }
        if (selfieFaces.isEmpty()) {
            return rejected("No face detected in the selfie.", livenessAction, false);
        }
        if (selfieFaces.size() > 1) {
            return rejected("Only one person should appear in the selfie.", livenessAction, false);
        }
        if (licenseFaces.size() > 1) {
            return new FaceMatchResult(
                    false,
                    0.0,
                    "More than one face was detected in the license image. Your documents need manual review.",
                    VerificationStatus.UNDER_REVIEW,
                    livenessAction,
                    false);
        }

        Face licenseFace = largestFace(licenseFaces);
        Face selfieFace = selfieFaces.get(0);

        LivenessCheck livenessCheck = checkLiveness(selfieFace, livenessAction);
        if (!livenessCheck.passed) {
            return new FaceMatchResult(
                    false,
                    0.0,
                    livenessCheck.message,
                    VerificationStatus.REJECTED,
                    livenessAction,
                    false);
        }

        if (!hasRequiredLandmarks(licenseFace) || !hasRequiredLandmarks(selfieFace)) {
            return new FaceMatchResult(
                    false,
                    0.0,
                    "Face detected, but image quality was not enough for automatic approval. Your documents need manual review.",
                    VerificationStatus.UNDER_REVIEW,
                    livenessAction,
                    true);
        }

        double score = computeSimilarity(licenseFace, selfieFace);
        boolean matched = score >= MATCH_THRESHOLD;
        String message = matched
                ? "Face match passed."
                : "The selfie does not match the driver license photo.";
        return new FaceMatchResult(
                matched,
                score,
                message,
                matched ? VerificationStatus.APPROVED : VerificationStatus.REJECTED,
                livenessAction,
                true);
    }

    private FaceMatchResult rejected(@NonNull String message,
                                     @NonNull LivenessAction livenessAction,
                                     boolean livenessPassed) {
        return new FaceMatchResult(
                false,
                0.0,
                message,
                VerificationStatus.REJECTED,
                livenessAction,
                livenessPassed);
    }

    private LivenessCheck checkLiveness(@NonNull Face face,
                                        @NonNull LivenessAction livenessAction) {
        switch (livenessAction) {
            case TURN_LEFT:
                if (face.getHeadEulerAngleY() <= -MIN_HEAD_TURN_DEGREES) {
                    return LivenessCheck.passed();
                }
                return LivenessCheck.failed("Liveness check failed. Please turn your face left and retake the selfie.");
            case TURN_RIGHT:
                if (face.getHeadEulerAngleY() >= MIN_HEAD_TURN_DEGREES) {
                    return LivenessCheck.passed();
                }
                return LivenessCheck.failed("Liveness check failed. Please turn your face right and retake the selfie.");
            case SMILE:
            default:
                Float smilingProbability = face.getSmilingProbability();
                if (smilingProbability != null && smilingProbability >= MIN_SMILE_PROBABILITY) {
                    return LivenessCheck.passed();
                }
                return LivenessCheck.failed("Liveness check failed. Please smile clearly and retake the selfie.");
        }
    }

    private static final class LivenessCheck {
        private final boolean passed;
        private final String message;

        private LivenessCheck(boolean passed, @NonNull String message) {
            this.passed = passed;
            this.message = message;
        }

        private static LivenessCheck passed() {
            return new LivenessCheck(true, "");
        }

        private static LivenessCheck failed(@NonNull String message) {
            return new LivenessCheck(false, message);
        }
    }

    private Face largestFace(@NonNull List<Face> faces) {
        Face largest = faces.get(0);
        int largestArea = area(largest.getBoundingBox());
        for (Face face : faces) {
            int area = area(face.getBoundingBox());
            if (area > largestArea) {
                largest = face;
                largestArea = area;
            }
        }
        return largest;
    }

    private int area(@NonNull Rect box) {
        return Math.max(0, box.width()) * Math.max(0, box.height());
    }

    private boolean hasRequiredLandmarks(@NonNull Face face) {
        return face.getLandmark(FaceLandmark.LEFT_EYE) != null
                && face.getLandmark(FaceLandmark.RIGHT_EYE) != null
                && face.getLandmark(FaceLandmark.NOSE_BASE) != null
                && face.getLandmark(FaceLandmark.MOUTH_LEFT) != null
                && face.getLandmark(FaceLandmark.MOUTH_RIGHT) != null;
    }

    private double computeSimilarity(@NonNull Face first, @NonNull Face second) {
        double eyeDistanceDelta = normalizedDistanceDelta(first, second,
                FaceLandmark.LEFT_EYE, FaceLandmark.RIGHT_EYE);
        double noseToEyeDelta = normalizedCenterDistanceDelta(first, second,
                FaceLandmark.NOSE_BASE, FaceLandmark.LEFT_EYE, FaceLandmark.RIGHT_EYE);
        double mouthWidthDelta = normalizedDistanceDelta(first, second,
                FaceLandmark.MOUTH_LEFT, FaceLandmark.MOUTH_RIGHT);
        double aspectDelta = Math.abs(faceAspect(first) - faceAspect(second));
        double headAngleDelta = (Math.abs(first.getHeadEulerAngleY() - second.getHeadEulerAngleY())
                + Math.abs(first.getHeadEulerAngleZ() - second.getHeadEulerAngleZ())) / 90.0;

        double distance = eyeDistanceDelta * 0.25
                + noseToEyeDelta * 0.25
                + mouthWidthDelta * 0.20
                + aspectDelta * 0.15
                + headAngleDelta * 0.15;

        return Math.max(0.0, Math.min(1.0, 1.0 - distance));
    }

    private double normalizedDistanceDelta(@NonNull Face first,
                                           @NonNull Face second,
                                           int leftType,
                                           int rightType) {
        double firstValue = distance(first, leftType, rightType) / Math.max(1.0, first.getBoundingBox().width());
        double secondValue = distance(second, leftType, rightType) / Math.max(1.0, second.getBoundingBox().width());
        return Math.abs(firstValue - secondValue);
    }

    private double normalizedCenterDistanceDelta(@NonNull Face first,
                                                 @NonNull Face second,
                                                 int centerType,
                                                 int leftType,
                                                 int rightType) {
        double firstValue = centerDistance(first, centerType, leftType, rightType)
                / Math.max(1.0, first.getBoundingBox().height());
        double secondValue = centerDistance(second, centerType, leftType, rightType)
                / Math.max(1.0, second.getBoundingBox().height());
        return Math.abs(firstValue - secondValue);
    }

    private double distance(@NonNull Face face, int firstType, int secondType) {
        PointF first = face.getLandmark(firstType).getPosition();
        PointF second = face.getLandmark(secondType).getPosition();
        return Math.hypot(first.x - second.x, first.y - second.y);
    }

    private double centerDistance(@NonNull Face face, int centerType, int leftType, int rightType) {
        PointF center = face.getLandmark(centerType).getPosition();
        PointF left = face.getLandmark(leftType).getPosition();
        PointF right = face.getLandmark(rightType).getPosition();
        PointF midpoint = new PointF((left.x + right.x) / 2f, (left.y + right.y) / 2f);
        return Math.hypot(center.x - midpoint.x, center.y - midpoint.y);
    }

    private double faceAspect(@NonNull Face face) {
        Rect box = face.getBoundingBox();
        return box.width() / Math.max(1.0, box.height());
    }
}
