package com.example.carrentingtest.storage;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StoragePathsTest {
    @Test
    public void paymentProofPath_includesRequestId() {
        String path = StoragePaths.paymentProofPath("abc123");
        assertEquals("rental_payments/abc123/proof.jpg", path);
    }

    @Test
    public void paymentProofPath_trimsWhitespace() {
        String path = StoragePaths.paymentProofPath("  abc123  ");
        assertEquals("rental_payments/abc123/proof.jpg", path);
    }

    @Test(expected = IllegalArgumentException.class)
    public void paymentProofPath_rejectsBlankId() {
        StoragePaths.paymentProofPath("   ");
    }
}
