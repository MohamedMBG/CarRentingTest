package com.example.carrentingtest.verification.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.carrentingtest.verification.VerificationStatus;

public class FaceMatchResult {
    private final boolean matched;
    private final double score;
    private final String message;
    private final VerificationStatus recommendedStatus;
    private final LivenessAction livenessAction;
    private final boolean livenessPassed;

    public FaceMatchResult(boolean matched, double score, String message) {
        this(matched, score, message, matched ? VerificationStatus.APPROVED : VerificationStatus.REJECTED);
    }

    public FaceMatchResult(boolean matched,
                           double score,
                           String message,
                           @NonNull VerificationStatus recommendedStatus) {
        this(matched, score, message, recommendedStatus, null, false);
    }

    public FaceMatchResult(boolean matched,
                           double score,
                           String message,
                           @NonNull VerificationStatus recommendedStatus,
                           @Nullable LivenessAction livenessAction,
                           boolean livenessPassed) {
        this.matched = matched;
        this.score = score;
        this.message = message;
        this.recommendedStatus = recommendedStatus;
        this.livenessAction = livenessAction;
        this.livenessPassed = livenessPassed;
    }

    public boolean isMatched() {
        return matched;
    }

    public double getScore() {
        return score;
    }

    public String getMessage() {
        return message;
    }

    @NonNull
    public VerificationStatus getRecommendedStatus() {
        return recommendedStatus;
    }

    @Nullable
    public LivenessAction getLivenessAction() {
        return livenessAction;
    }

    public boolean isLivenessPassed() {
        return livenessPassed;
    }
}
