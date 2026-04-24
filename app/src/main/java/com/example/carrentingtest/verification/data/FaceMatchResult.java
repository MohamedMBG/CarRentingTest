package com.example.carrentingtest.verification.data;

public class FaceMatchResult {
    private final boolean matched;
    private final double score;
    private final String message;

    public FaceMatchResult(boolean matched, double score, String message) {
        this.matched = matched;
        this.score = score;
        this.message = message;
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
}
