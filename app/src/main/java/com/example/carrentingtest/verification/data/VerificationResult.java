package com.example.carrentingtest.verification.data;

public class VerificationResult {
    public enum Status { SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED }
    private final Status status;
    private final double faceMatchScore;
    private final String message;

    public VerificationResult(Status status) {
        this(status, 0.0, null);
    }

    public VerificationResult(Status status, double faceMatchScore, String message) {
        this.status = status;
        this.faceMatchScore = faceMatchScore;
        this.message = message;
    }

    public Status getStatus() { return status; }

    public double getFaceMatchScore() { return faceMatchScore; }

    public String getMessage() { return message; }
}


