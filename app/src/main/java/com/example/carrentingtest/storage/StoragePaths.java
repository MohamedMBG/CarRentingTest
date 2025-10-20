package com.example.carrentingtest.storage;

public final class StoragePaths {
    private StoragePaths() {}

    public static String selfiePath(String uid) {
        return "verifications/" + uid + "/selfie.jpg";
    }

    public static String licenseFrontPath(String uid) {
        return "verifications/" + uid + "/license_front.jpg";
    }

    public static String licenseBackPath(String uid) {
        return "verifications/" + uid + "/license_back.jpg";
    }

    public static String paymentProofPath(String requestId) {
        if (requestId == null) {
            throw new IllegalArgumentException("requestId must not be null or empty");
        }
        String sanitized = requestId.trim();
        if (sanitized.isEmpty()) {
            throw new IllegalArgumentException("requestId must not be null or empty");
        }
        return "rental_payments/" + sanitized + "/proof.jpg";
    }
}


