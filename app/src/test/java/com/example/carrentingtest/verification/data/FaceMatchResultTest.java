package com.example.carrentingtest.verification.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.carrentingtest.verification.VerificationStatus;

import org.junit.Test;

public class FaceMatchResultTest {
    @Test
    public void constructorDefaultsMatchedToApproved() {
        FaceMatchResult result = new FaceMatchResult(true, 0.9, "passed");

        assertTrue(result.isMatched());
        assertEquals(VerificationStatus.APPROVED, result.getRecommendedStatus());
    }

    @Test
    public void constructorDefaultsUnmatchedToRejected() {
        FaceMatchResult result = new FaceMatchResult(false, 0.2, "failed");

        assertFalse(result.isMatched());
        assertEquals(VerificationStatus.REJECTED, result.getRecommendedStatus());
    }

    @Test
    public void canRepresentManualReviewWithoutMatch() {
        FaceMatchResult result = new FaceMatchResult(
                false,
                0.0,
                "manual review",
                VerificationStatus.UNDER_REVIEW);

        assertFalse(result.isMatched());
        assertEquals(VerificationStatus.UNDER_REVIEW, result.getRecommendedStatus());
    }

    @Test
    public void storesLivenessMetadata() {
        FaceMatchResult result = new FaceMatchResult(
                true,
                0.95,
                "passed",
                VerificationStatus.APPROVED,
                LivenessAction.SMILE,
                true);

        assertTrue(result.isLivenessPassed());
        assertEquals(LivenessAction.SMILE, result.getLivenessAction());
    }
}
