package com.example.carrentingtest.verification.data;

public class VerificationResult {
    public enum Status { VERIFIED, PENDING, FAILED }
    private final Status status;

    public VerificationResult(Status status) { this.status = status; }

    public Status getStatus() { return status; }
}


