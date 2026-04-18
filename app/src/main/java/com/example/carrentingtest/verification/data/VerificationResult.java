package com.example.carrentingtest.verification.data;

public class VerificationResult {
    public enum Status { SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED }
    private final Status status;

    public VerificationResult(Status status) { this.status = status; }

    public Status getStatus() { return status; }
}


